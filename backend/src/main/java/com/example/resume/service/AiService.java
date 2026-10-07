package com.example.resume.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.resume.dto.AiChatRequest;
import com.example.resume.entity.*;
import com.example.resume.mapper.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * AI 智能问答服务：
 * 1. 以指定（或默认）简历版本的全部内容构建知识库；
 * 2. 携带同会话、同版本最近几轮历史，调用 OpenAI 兼容的大模型接口；
 * 3. 未配置 api-key 或调用失败时，降级为基于简历内容的本地关键词问答；
 * 4. 每轮问答写入 ai_chat_history，供后台管理。
 */
@Slf4j
@Service
public class AiService {

    private static final int HISTORY_LIMIT = 6;

    private final ProfileMapper profileMapper;
    private final EducationMapper educationMapper;
    private final ExperienceMapper experienceMapper;
    private final SkillMapper skillMapper;
    private final HonorMapper honorMapper;
    private final PortfolioMapper portfolioMapper;
    private final AiChatHistoryMapper chatHistoryMapper;
    private final VersionService versionService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    @Value("${app.ai.api-url}")
    private String apiUrl;

    @Value("${app.ai.api-key}")
    private String apiKey;

    @Value("${app.ai.model}")
    private String model;

    @Value("${app.ai.timeout}")
    private long timeout;

    @Value("${app.ai.embed-url:}")
    private String embedUrl;

    @Value("${app.ai.embed-model:}")
    private String embedModel;

    @Value("${app.ai.embed-batch-size:16}")
    private int embedBatchSize;

    @Value("${app.ai.retrieve-top-k:6}")
    private int retrieveTopK;

    /** 各简历版本的向量知识库缓存（内容变更后按签名自动重建） */
    private final Map<Long, KbCache> vectorCache = new ConcurrentHashMap<>();

    /** 知识块向量缓存：signature 为知识库文本摘要，内容一改即失效 */
    private record KbCache(String signature, List<String> chunks, List<float[]> vectors) {
    }

    public AiService(ProfileMapper profileMapper,
                     EducationMapper educationMapper,
                     ExperienceMapper experienceMapper,
                     SkillMapper skillMapper,
                     HonorMapper honorMapper,
                     PortfolioMapper portfolioMapper,
                     AiChatHistoryMapper chatHistoryMapper,
                     VersionService versionService) {
        this.profileMapper = profileMapper;
        this.educationMapper = educationMapper;
        this.experienceMapper = experienceMapper;
        this.skillMapper = skillMapper;
        this.honorMapper = honorMapper;
        this.portfolioMapper = portfolioMapper;
        this.chatHistoryMapper = chatHistoryMapper;
        this.versionService = versionService;
    }

    /**
     * 问答入口
     */
    public Map<String, Object> chat(AiChatRequest request) {
        Long versionId = versionService.resolveVersionId(request.getVersionId());
        String knowledgeBase = buildKnowledgeBase(versionId);
        String sessionId = request.getSessionId() != null && !request.getSessionId().isBlank()
                ? request.getSessionId() : UUID.randomUUID().toString().replace("-", "");

        String answer;
        String source;
        // 上下文回捞键带版本号：同一浏览器会话换版本时不复用上一版本的多轮记录。
        // session_id 列宽 64，先把 sessionId 截短再拼后缀，避免后缀被截掉后不同版本重新并流
        String versionSuffix = "#" + versionId;
        String historyKey = sessionId.length() + versionSuffix.length() > 64
                ? sessionId.substring(0, 64 - versionSuffix.length()) + versionSuffix
                : sessionId + versionSuffix;
        if (apiKey != null && !apiKey.isBlank()) {
            try {
                // 先用向量嵌入做语义检索，只把最相关的知识块交给大模型
                String context = retrieveContext(request.getQuestion(), versionId, knowledgeBase);
                answer = callLlm(request.getQuestion(), context, historyKey);
                source = "llm";
            } catch (Exception e) {
                log.warn("大模型调用失败，降级本地问答: {}", e.getMessage());
                answer = localAnswer(request.getQuestion(), knowledgeBase, versionId);
                source = "local";
            }
        } else {
            answer = localAnswer(request.getQuestion(), knowledgeBase, versionId);
            source = "local";
        }

        AiChatHistory history = new AiChatHistory();
        history.setSessionId(historyKey);
        history.setQuestion(request.getQuestion());
        history.setAnswer(answer);
        history.setSource(source);
        chatHistoryMapper.insert(history);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("sessionId", sessionId);
        result.put("answer", answer);
        result.put("source", source);
        return result;
    }

    /**
     * 从简历所有数据表拼装知识库文本
     */
    private String buildKnowledgeBase(Long versionId) {
        StringBuilder sb = new StringBuilder();

        Profile profile = profileMapper.selectOne(new LambdaQueryWrapper<Profile>()
                .eq(Profile::getVersionId, versionId).last("LIMIT 1"));
        if (profile != null) {
            sb.append("【基本信息】\n");
            appendIfPresent(sb, "姓名", profile.getName());
            appendIfPresent(sb, "职位", profile.getJobTitle());
            appendIfPresent(sb, "一句话简介", profile.getSlogan());
            appendIfPresent(sb, "邮箱", profile.getEmail());
            appendIfPresent(sb, "电话", profile.getPhone());
            appendIfPresent(sb, "微信", profile.getWechat());
            appendIfPresent(sb, "GitHub", profile.getGithub());
            appendIfPresent(sb, "Gitee", profile.getGitee());
            appendIfPresent(sb, "CSDN", profile.getCsdn());
            appendIfPresent(sb, "所在地", profile.getAddress());
            appendIfPresent(sb, "个人简介", profile.getAbout());
            sb.append('\n');
        }

        List<Education> educations = educationMapper.selectList(new LambdaQueryWrapper<Education>()
                .eq(Education::getVersionId, versionId)
                .orderByAsc(Education::getSort).orderByDesc(Education::getStartDate));
        if (!educations.isEmpty()) {
            sb.append("【教育经历】\n");
            for (Education e : educations) {
                sb.append("- ").append(nullToEmpty(e.getSchool()))
                        .append(' ').append(nullToEmpty(e.getMajor()))
                        .append(' ').append(nullToEmpty(e.getDegree()))
                        .append("（").append(dateText(e.getStartDate())).append('~')
                        .append(dateText(e.getEndDate())).append("）")
                        .append(nullToEmpty(e.getDescription())).append('\n');
            }
        }

        List<Experience> experiences = experienceMapper.selectList(new LambdaQueryWrapper<Experience>()
                .eq(Experience::getVersionId, versionId)
                .orderByAsc(Experience::getSort).orderByDesc(Experience::getStartDate));
        List<Experience> works = experiences.stream().filter(e -> Integer.valueOf(1).equals(e.getType())).toList();
        List<Experience> projects = experiences.stream().filter(e -> Integer.valueOf(2).equals(e.getType())).toList();
        appendExperiences(sb, "工作经历", works);
        appendExperiences(sb, "项目经历", projects);

        List<Skill> skills = skillMapper.selectList(new LambdaQueryWrapper<Skill>()
                .eq(Skill::getVersionId, versionId).orderByAsc(Skill::getSort));
        if (!skills.isEmpty()) {
            sb.append("【技能特长】\n");
            Map<String, List<String>> grouped = new LinkedHashMap<>();
            for (Skill s : skills) {
                grouped.computeIfAbsent(
                                s.getCategory() == null ? "其他" : s.getCategory(),
                                k -> new ArrayList<>())
                        .add(s.getName() + "（熟练度" + s.getLevel() + "%）");
            }
            grouped.forEach((category, names) ->
                    sb.append("- ").append(category).append("：")
                            .append(String.join("、", names)).append('\n'));
        }

        // 荣誉证书、作品集均按简历主人姓名归属：先取该版本姓名
        Profile contentOwner = profileMapper.selectOne(new LambdaQueryWrapper<Profile>()
                .eq(Profile::getVersionId, versionId).last("LIMIT 1"));
        String ownerName = contentOwner == null ? null : contentOwner.getName();

        List<Honor> honors = ownerName != null
                ? honorMapper.selectList(new LambdaQueryWrapper<Honor>()
                        .eq(Honor::getOwnerName, ownerName)
                        .orderByAsc(Honor::getSort).orderByAsc(Honor::getId))
                : Collections.emptyList();
        if (!honors.isEmpty()) {
            sb.append("【荣誉证书】\n");
            for (Honor h : honors) {
                sb.append("- ").append(nullToEmpty(h.getTitle()));
                if (h.getLevel() != null && !h.getLevel().isBlank()) {
                    sb.append("（").append(h.getLevel()).append("）");
                }
                if (h.getDescription() != null && !h.getDescription().isBlank()) {
                    sb.append("：").append(h.getDescription());
                }
                sb.append('\n');
            }
        }

        // 作品集按简历主人姓名归属：返回其名下全部作品
        List<Portfolio> portfolios = ownerName != null
                ? portfolioMapper.selectList(new LambdaQueryWrapper<Portfolio>()
                        .eq(Portfolio::getOwnerName, ownerName)
                        .orderByAsc(Portfolio::getSort).orderByAsc(Portfolio::getId))
                : Collections.emptyList();
        if (!portfolios.isEmpty()) {
            sb.append("【作品集】\n");
            for (Portfolio p : portfolios) {
                sb.append("- ").append(nullToEmpty(p.getTitle()))
                        .append("：").append(nullToEmpty(p.getDescription()));
                if (p.getUrl() != null && !p.getUrl().isBlank()) {
                    sb.append("（链接：").append(p.getUrl()).append("）");
                }
                sb.append('\n');
            }
        }
        return sb.toString();
    }

    private void appendExperiences(StringBuilder sb, String title, List<Experience> list) {
        if (list.isEmpty()) {
            return;
        }
        sb.append("【").append(title).append("】\n");
        for (Experience e : list) {
            sb.append("- ").append(nullToEmpty(e.getCompany()))
                    .append(' ').append(nullToEmpty(e.getPosition()))
                    .append("（").append(dateText(e.getStartDate())).append('~')
                    .append(dateText(e.getEndDate())).append("）\n");
            if (e.getDescription() != null && !e.getDescription().isBlank()) {
                sb.append("  职责/描述：").append(e.getDescription()).append('\n');
            }
            if (e.getTechStack() != null && !e.getTechStack().isBlank()) {
                sb.append("  技术栈：").append(e.getTechStack()).append('\n');
            }
        }
    }

    // ==================== RAG 向量检索 ====================

    /**
     * 语义检索：把问题向量化，与缓存的简历知识块做余弦相似度匹配，
     * 返回最相关的若干知识块拼成的上下文。
     * 嵌入服务不可用或未配置时，回退为全量知识库，不影响问答主流程。
     */
    private String retrieveContext(String question, Long versionId, String knowledgeBase) {
        if (embedUrl == null || embedUrl.isBlank() || question == null || question.isBlank()) {
            return knowledgeBase;
        }
        try {
            KbCache cache = getOrBuildCache(versionId, knowledgeBase);
            if (cache.chunks().isEmpty()) {
                return knowledgeBase;
            }
            float[] queryVector = embedTexts(List.of(question.trim())).get(0);

            // 基本信息块始终作为上下文（姓名、求职意向、联系方式等事实基础）
            LinkedHashSet<Integer> selected = new LinkedHashSet<>();
            if (!cache.chunks().isEmpty() && cache.chunks().get(0).startsWith("【基本信息】")) {
                selected.add(0);
            }

            List<ScoredChunk> scored = new ArrayList<>();
            for (int i = 0; i < cache.chunks().size(); i++) {
                scored.add(new ScoredChunk(i, cosineSimilarity(queryVector, cache.vectors().get(i))));
            }
            scored.sort(Comparator.comparingDouble(ScoredChunk::score).reversed());
            for (ScoredChunk sc : scored) {
                if (selected.size() >= retrieveTopK) {
                    break;
                }
                // 相似度过低的块不纳入，避免无关内容干扰回答
                if (sc.score() >= 0.15 || selected.isEmpty()) {
                    selected.add(sc.index());
                }
            }

            StringBuilder ctx = new StringBuilder();
            for (Integer idx : selected) {
                ctx.append(cache.chunks().get(idx)).append("\n\n");
            }
            String context = ctx.toString().trim();
            log.debug("RAG 检索命中 {} 个知识块（问题：{}）", selected.size(), question);
            return context.isEmpty() ? knowledgeBase : context;
        } catch (Exception e) {
            log.warn("向量检索失败，使用全量知识库: {}", e.getMessage());
            return knowledgeBase;
        }
    }

    private record ScoredChunk(int index, double score) {
    }

    /** 按知识库内容签名缓存向量，简历内容变更后自动重建 */
    private KbCache getOrBuildCache(Long versionId, String knowledgeBase) throws Exception {
        String signature = kbSignature(knowledgeBase);
        KbCache cached = vectorCache.get(versionId);
        if (cached != null && cached.signature().equals(signature)) {
            return cached;
        }
        synchronized (vectorCache) {
            cached = vectorCache.get(versionId);
            if (cached != null && cached.signature().equals(signature)) {
                return cached;
            }
            List<String> chunks = chunkKnowledgeBase(knowledgeBase);
            List<float[]> vectors = embedTexts(chunks);
            KbCache fresh = new KbCache(signature, chunks, vectors);
            vectorCache.put(versionId, fresh);
            log.info("简历版本 {} 向量知识库已构建，共 {} 个知识块", versionId, chunks.size());
            return fresh;
        }
    }

    /**
     * 知识分块：
     * - 基本信息整体作为一个块；
     * - 其余栏目每条记录（含其下方缩进的职责/技术栈描述）作为一个块，并带上栏目标题。
     */
    List<String> chunkKnowledgeBase(String knowledgeBase) {
        List<String> chunks = new ArrayList<>();
        String section = "";
        StringBuilder basicBlock = new StringBuilder();
        boolean inBasic = false;

        for (String rawLine : knowledgeBase.split("\n")) {
            String line = rawLine.trim();
            if (line.isEmpty()) {
                continue;
            }
            if (line.startsWith("【") && line.endsWith("】")) {
                section = line;
                inBasic = "【基本信息】".equals(line);
                if (inBasic) {
                    basicBlock.append(line).append('\n');
                }
                continue;
            }
            if (inBasic) {
                basicBlock.append(line).append('\n');
            } else if (line.startsWith("- ")) {
                chunks.add(section + " " + line.substring(2).trim());
            } else if (line.startsWith("职责/描述：") || line.startsWith("技术栈：")
                    || rawLine.startsWith("  ")) {
                // 上一条记录的续行，并入最后一个块
                if (!chunks.isEmpty()) {
                    int last = chunks.size() - 1;
                    chunks.set(last, chunks.get(last) + "；" + line);
                } else {
                    chunks.add(section + " " + line);
                }
            } else {
                chunks.add(section + " " + line);
            }
        }
        if (basicBlock.length() > 0) {
            chunks.add(0, basicBlock.toString().trim());
        }
        return chunks;
    }

    /** 调用 Embedding 接口，批量获取文本向量（按 embed-batch-size 分批） */
    private List<float[]> embedTexts(List<String> texts) throws Exception {
        List<float[]> result = new ArrayList<>(Collections.nCopies(texts.size(), new float[0]));
        for (int start = 0; start < texts.size(); start += Math.max(1, embedBatchSize)) {
            int end = Math.min(start + Math.max(1, embedBatchSize), texts.size());
            List<String> batch = texts.subList(start, end);

            Map<String, Object> body = new LinkedHashMap<>();
            body.put("model", embedModel);
            body.put("input", batch);
            body.put("encoding_format", "float");

            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(embedUrl))
                    .timeout(Duration.ofMillis(timeout))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body),
                            StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = httpClient.send(httpRequest,
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("嵌入接口返回 " + response.statusCode()
                        + ": " + response.body());
            }
            JsonNode root = objectMapper.readTree(response.body());
            JsonNode data = root.path("data");
            if (!data.isArray() || data.isEmpty()) {
                throw new IllegalStateException("嵌入接口返回数据为空: " + response.body());
            }
            for (JsonNode item : data) {
                int index = item.path("index").asInt(-1);
                if (index < 0 || start + index >= end) {
                    continue;
                }
                JsonNode emb = item.path("embedding");
                float[] vector = new float[emb.size()];
                for (int i = 0; i < emb.size(); i++) {
                    vector[i] = (float) emb.get(i).asDouble();
                }
                result.set(start + index, vector);
            }
        }
        for (float[] v : result) {
            if (v == null || v.length == 0) {
                throw new IllegalStateException("部分文本嵌入向量缺失");
            }
        }
        return result;
    }

    /** 余弦相似度 */
    private double cosineSimilarity(float[] a, float[] b) {
        if (a.length != b.length) {
            return 0;
        }
        double dot = 0;
        double normA = 0;
        double normB = 0;
        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }
        if (normA == 0 || normB == 0) {
            return 0;
        }
        return dot / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    /** 知识库内容签名（SHA-256 前 16 位） */
    private String kbSignature(String content) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(content.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : digest) {
                hex.append(String.format("%02x", b));
            }
            return hex.substring(0, 16);
        } catch (Exception e) {
            return String.valueOf(content.hashCode());
        }
    }

    /**
     * 调用 OpenAI 兼容 Chat Completions 接口
     */
    private String callLlm(String question, String knowledgeBase, String historyKey) throws Exception {
        List<Map<String, String>> messages = new ArrayList<>();

        java.time.LocalDate today = java.time.LocalDate.now();
        String todayText = today.getYear() + "年" + today.getMonthValue() + "月" + today.getDayOfMonth() + "日";

        Map<String, String> system = new LinkedHashMap<>();
        system.put("role", "system");
        system.put("content", "你是部署在个人简历网站上的智能助理，只能依据下面提供的简历知识库回答访客问题。"
                + "回答使用简体中文，语气友好专业；如果知识库中没有相关信息，要如实说明，不要编造。\n\n"
                + "【时间判断规则，必须严格遵守】\n"
                + "1. 当前日期是 " + todayText + "，回答涉及毕业、在校、工作年限、年龄等时间相关问题时，"
                + "必须以当前日期为基准推算，不要假设其他时间。\n"
                + "2. 知识库中教育经历的结束日期即毕业时间；只要毕业日期早于或等于当前日期，就应回答“已经毕业”，"
                + "并明确毕业院校与时间；毕业日期晚于当前日期时才回答“尚未毕业/在读”。\n"
                + "3. 简历正文中若出现“预计毕业”“将于……毕业”等表述，但所述日期已过，以实际日期为准，按已毕业回答。\n\n"
                + "简历知识库：\n" + knowledgeBase);
        messages.add(system);

        // 追加同会话、同版本的历史，实现多轮对话
        List<AiChatHistory> histories = chatHistoryMapper.selectList(
                new LambdaQueryWrapper<AiChatHistory>()
                        .eq(AiChatHistory::getSessionId, historyKey)
                        .orderByDesc(AiChatHistory::getId)
                        .last("LIMIT " + HISTORY_LIMIT));
        Collections.reverse(histories);
        for (AiChatHistory h : histories) {
            messages.add(Map.of("role", "user", "content", h.getQuestion()));
            messages.add(Map.of("role", "assistant", "content", h.getAnswer()));
        }
        messages.add(Map.of("role", "user", "content", question));

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", model);
        body.put("messages", messages);
        body.put("temperature", 0.7);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl))
                .timeout(Duration.ofMillis(timeout))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                .build();

        HttpResponse<String> response = httpClient.send(request,
                HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException("大模型接口返回 " + response.statusCode()
                    + ": " + response.body());
        }
        JsonNode root = objectMapper.readTree(response.body());
        JsonNode content = root.path("choices").path(0).path("message").path("content");
        if (content.isMissingNode() || content.asText().isBlank()) {
            throw new IllegalStateException("大模型返回内容为空");
        }
        return content.asText();
    }

    /**
     * 本地降级问答：意图识别 + 知识库全文检索 + 简历速览兜底，
     * 保证未配置大模型时，访客的绝大多数提问都能得到基于简历的实质性回答。
     */
    private String localAnswer(String question, String knowledgeBase, Long versionId) {
        Profile profile = profileMapper.selectOne(new LambdaQueryWrapper<Profile>()
                .eq(Profile::getVersionId, versionId).last("LIMIT 1"));
        String name = profile != null && !nullToEmpty(profile.getName()).isBlank()
                ? profile.getName() : "候选人";
        String q = question == null ? "" : question.trim();
        if (q.isBlank()) {
            return greetingAnswer(name, profile);
        }
        String ql = q.toLowerCase();

        // 1. 纯问候
        if (containsAny(ql, "你好", "您好", "hello", "hi", "嗨", "哈喽", "在吗", "在么", "在不在")) {
            return greetingAnswer(name, profile);
        }

        // 2. 出生 / 年龄（属于基本资料的第一行）
        if (containsAny(ql, "出生", "生日", "年龄", "多大", "几岁", "哪年生")) {
            String firstPara = firstParagraph(profile);
            return "根据简历记录：\n" + (firstPara.isBlank() ? "暂未维护出生日期信息。" : firstPara);
        }

        // 3. 联系方式
        if (containsAny(ql, "联系方式", "邮箱", "邮件", "电话", "手机", "号码", "微信",
                "github", "gitee", "csdn", "博客", "代码仓库", "怎么联系", "如何联系",
                "联系她", "联系他", "怎么找她", "怎么找他")) {
            if (profile == null) {
                return "暂未维护联系方式。";
            }
            List<String> lines = new ArrayList<>();
            addLine(lines, "邮箱", profile.getEmail());
            addLine(lines, "电话", profile.getPhone());
            addLine(lines, "微信", profile.getWechat());
            addLine(lines, "GitHub", profile.getGithub());
            addLine(lines, "Gitee", profile.getGitee());
            addLine(lines, "CSDN", profile.getCsdn());
            addLine(lines, "所在地", profile.getAddress());
            return lines.isEmpty() ? "暂未维护联系方式。"
                    : "你可以通过以下方式联系" + name + "：\n" + String.join("\n", lines);
        }

        // 4. 荣誉证书
        if (containsAny(ql, "荣誉", "证书", "获奖", "奖项", "奖学金", "竞赛", "比赛",
                "奖状", "颁奖", "名次", "等次", "几等", "志愿者", "拿过什么奖")) {
            String section = extractSection(knowledgeBase, "【荣誉证书】");
            return section.isBlank() ? "暂未维护荣誉证书信息。" : section;
        }

        // 5. 工作 / 实习经历
        if (containsAny(ql, "实习", "工作经历", "工作经验", "工作做什么", "哪家公司",
                "哪个公司", "什么公司", "公司", "任职", "职业经历", "上班", "单位")) {
            String section = extractSection(knowledgeBase, "【工作经历】");
            return section.isBlank() ? "暂未维护工作/实习经历。" : section;
        }

        // 6. 项目 / 作品
        if (containsAny(ql, "项目", "作品", "做过", "实践项目", "负责过",
                "博客", "大屏", "编辑器", "小程序")) {
            String projects = extractSection(knowledgeBase, "【项目经历】");
            String portfolios = extractSection(knowledgeBase, "【作品集】");
            String result = projects + (portfolios.isBlank() ? "" : "\n" + portfolios);
            return result.isBlank() ? "暂未维护项目/作品经历。" : result.trim();
        }

        // 7. 技能 / 工具
        if (containsAny(ql, "技能", "技术栈", "会什么", "会哪些", "会啥", "擅长",
                "能力", "水平", "工具", "postman", "adb", "logcat", "iperf", "linux",
                "vue", "spring", "uniapp", "uni-app", "黑盒", "白盒", "自动化",
                "性能测试", "兼容性测试", "回归测试")) {
            return extractSection(knowledgeBase, "【技能特长】");
        }

        // 8. 教育背景
        if (containsAny(ql, "教育", "毕业", "学校", "学历", "专业", "大学", "学院",
                "本科", "课程", "上学", "读书")) {
            return extractSection(knowledgeBase, "【教育经历】");
        }

        // 9. 求职意向
        if (containsAny(ql, "求职", "意向岗位", "岗位意向", "找工作", "想找", "期望",
                "应聘", "想做什么工作", "招什么岗位", "求职岗位")) {
            String title = profile != null ? nullToEmpty(profile.getJobTitle()) : "";
            String firstPara = firstParagraph(profile);
            StringBuilder sb = new StringBuilder("求职意向：")
                    .append(title.isBlank() ? "暂未维护" : title);
            if (!firstPara.isBlank()) {
                sb.append("\n").append(firstPara);
            }
            return sb.toString();
        }

        // 10. 自我评价
        if (containsAny(ql, "自我评价", "优点", "优势", "特点", "性格", "怎么样的人",
                "什么样的人", "核心竞争力", "长处", "靠不靠谱")) {
            String about = profile != null ? nullToEmpty(profile.getAbout()) : "";
            int idx = about.indexOf("自我评价");
            String eval = idx >= 0 ? about.substring(idx).replace("自我评价：", "").trim() : about;
            return eval.isBlank() ? "暂未维护自我评价。" : "自我评价：\n" + eval;
        }

        // 11. 身份 / 基本介绍
        if (containsAny(ql, "你是谁", "你是哪位", "自我介绍", "简介", "个人情况",
                "基本情况", "概况", "哪里人", "住哪", "地址", "老家", "所在地", "介绍一下她", "介绍一下他")) {
            String about = profile != null ? nullToEmpty(profile.getAbout()) : "";
            String title = profile != null ? nullToEmpty(profile.getJobTitle()) : "";
            StringBuilder sb = new StringBuilder();
            sb.append(name).append(title.isBlank() ? "" : "，" + title).append("。");
            if (!about.isBlank()) {
                sb.append('\n').append(about);
            }
            return sb.toString();
        }

        // 12. 全文检索兜底：在简历知识库中按问题关键词逐行匹配
        String hit = searchKnowledge(q, knowledgeBase);
        if (hit != null) {
            return hit;
        }

        // 13. 最终兜底：返回简历速览，而不是"回答不了"的空引导
        return overviewAnswer(name, profile, knowledgeBase);
    }

    /** 问候语：带上简历亮点与可问方向 */
    private String greetingAnswer(String name, Profile profile) {
        String title = profile != null ? nullToEmpty(profile.getJobTitle()) : "";
        return "你好！我是 " + name + " 的简历助手"
                + (title.isBlank() ? "" : "，求职意向是「" + title + "」") + "。\n"
                + "关于教育背景、实习与项目经历、技能特长、荣誉证书、联系方式都可以直接问，例如：\n"
                + "- 实习期间主要做什么？\n"
                + "- 掌握哪些技能？\n"
                + "- 做过哪些项目？\n"
                + "- 获得过哪些奖项？\n"
                + "- 什么时候毕业？怎么联系？";
    }

    /** 最终兜底：简历速览，确保任何问题都返回实质内容 */
    private String overviewAnswer(String name, Profile profile, String knowledgeBase) {
        StringBuilder sb = new StringBuilder();
        sb.append("这个问题我没在简历里找到完全对应的信息，先为你奉上 ").append(name).append(" 的简历速览：\n\n");
        sb.append("【基本信息】\n");
        if (profile != null) {
            appendIfPresent(sb, "姓名", profile.getName());
            appendIfPresent(sb, "求职意向", profile.getJobTitle());
            appendIfPresent(sb, "所在地", profile.getAddress());
        }
        String education = extractSection(knowledgeBase, "【教育经历】");
        if (!education.isBlank()) {
            sb.append('\n').append(education).append('\n');
        }
        String skill = extractSection(knowledgeBase, "【技能特长】");
        if (!skill.isBlank()) {
            sb.append('\n').append(skill).append('\n');
        }
        String work = extractSection(knowledgeBase, "【工作经历】");
        if (!work.isBlank()) {
            sb.append('\n').append(work).append('\n');
        }
        String projects = extractSection(knowledgeBase, "【项目经历】");
        if (!projects.isBlank()) {
            sb.append('\n').append(projects).append('\n');
        }
        String honors = extractSection(knowledgeBase, "【荣誉证书】");
        if (!honors.isBlank()) {
            sb.append('\n').append(honors).append('\n');
        }
        sb.append("\n你可以换个问法试试，比如：实习期间负责什么？掌握哪些技能？获得过什么奖项？怎么联系？");
        return sb.toString();
    }

    /**
     * 知识库逐行检索：把问题拆成英文词 + 中文 2/3-gram，
     * 对知识库每一行做包含计分，按段落聚合最相关的行返回。
     */
    private String searchKnowledge(String rawQuestion, String knowledgeBase) {
        List<String> tokens = new ArrayList<>();
        java.util.regex.Matcher wordMatcher = WORD_PATTERN.matcher(rawQuestion.toLowerCase());
        while (wordMatcher.find()) {
            tokens.add(wordMatcher.group());
        }
        String han = rawQuestion.replaceAll("[^\\p{IsHan}]", "");
        for (String stop : STOP_WORDS) {
            han = han.replace(stop, "");
        }
        if (han.length() >= 2) {
            tokens.add(han);
            for (int i = 0; i + 2 <= han.length(); i++) {
                tokens.add(han.substring(i, i + 2));
            }
            for (int i = 0; i + 3 <= han.length(); i++) {
                tokens.add(han.substring(i, i + 3));
            }
        }
        if (tokens.isEmpty()) {
            return null;
        }

        // 解析段落与行并计分
        String section = "";
        List<SearchHit> hits = new ArrayList<>();
        for (String rawLine : knowledgeBase.split("\n")) {
            String line = rawLine.trim();
            if (line.isEmpty()) {
                continue;
            }
            if (line.startsWith("【") && line.endsWith("】")) {
                section = line;
                continue;
            }
            String lower = line.toLowerCase();
            int score = 0;
            for (String token : tokens) {
                if (lower.contains(token)) {
                    score += token.length() >= 3 ? 2 : 1;
                }
            }
            if (score > 0) {
                hits.add(new SearchHit(section, line, score));
            }
        }
        if (hits.isEmpty()) {
            return null;
        }
        hits.sort(Comparator.comparingInt((SearchHit h) -> h.score).reversed());

        // 每段最多 2 行，总计最多 6 行
        Map<String, Integer> sectionQuota = new HashMap<>();
        List<SearchHit> picked = new ArrayList<>();
        for (SearchHit hit : hits) {
            int used = sectionQuota.getOrDefault(hit.section, 0);
            if (used >= 2) {
                continue;
            }
            sectionQuota.put(hit.section, used + 1);
            picked.add(hit);
            if (picked.size() >= 6) {
                break;
            }
        }

        // 按知识库原段落顺序聚合输出
        Map<String, List<String>> grouped = new LinkedHashMap<>();
        for (SearchHit hit : picked) {
            grouped.computeIfAbsent(hit.section, k -> new ArrayList<>()).add(hit.line);
        }
        StringBuilder sb = new StringBuilder("根据简历内容，为你找到以下相关信息：\n");
        grouped.forEach((sec, lines) -> {
            sb.append('\n').append(sec).append('\n');
            lines.forEach(line -> sb.append(line).append('\n'));
        });
        return sb.toString().trim();
    }

    /** 全文检索命中行 */
    private static final class SearchHit {
        final String section;
        final String line;
        final int score;

        SearchHit(String section, String line, int score) {
            this.section = section;
            this.line = line;
            this.score = score;
        }
    }

    /** 本地问答检索前剔除的口语/疑问词 */
    private static final String[] STOP_WORDS = {
            "请问", "一下", "什么", "怎么", "怎样", "如何", "哪些", "那个", "这个", "可以",
            "能不能", "是不是", "有没有", "知道", "了解", "告诉", "我想", "还有", "以及", "需要",
            "他的", "她的", "它的", "他们", "她们", "对方", "候选人", "本人",
            "的", "了", "吗", "呢", "啊", "呀", "吧", "么", "在", "是", "有", "和", "与", "或", "跟",
            "你", "我", "他", "她", "它", "们", "个", "些", "哪", "上", "下", "里", "中",
            "会", "能", "要", "想", "做", "用", "对", "从", "到", "把", "被", "让", "给",
            "很", "最", "更", "比", "如", "还", "也", "都", "就", "只", "又", "再", "已",
            "曾", "过", "着", "之", "其", "此", "问", "下", "看", "说", "讲", "啥"
    };

    private static final java.util.regex.Pattern WORD_PATTERN =
            java.util.regex.Pattern.compile("[a-zA-Z][a-zA-Z0-9+.#-]{1,}");

    private String extractSection(String knowledgeBase, String sectionTitle) {
        int start = knowledgeBase.indexOf(sectionTitle);
        if (start < 0) {
            return "";
        }
        int next = knowledgeBase.indexOf("【", start + sectionTitle.length());
        String section = next < 0
                ? knowledgeBase.substring(start)
                : knowledgeBase.substring(start, next);
        return section.trim();
    }

    private boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    public Page<AiChatHistory> adminPage(long current, long size, String sessionId) {
        LambdaQueryWrapper<AiChatHistory> wrapper = new LambdaQueryWrapper<AiChatHistory>()
                .orderByDesc(AiChatHistory::getId);
        if (sessionId != null && !sessionId.isBlank()) {
            wrapper.eq(AiChatHistory::getSessionId, sessionId);
        }
        return chatHistoryMapper.selectPage(new Page<>(current, size), wrapper);
    }

    public void delete(Long id) {
        chatHistoryMapper.deleteById(id);
    }

    public void clear() {
        chatHistoryMapper.delete(new LambdaQueryWrapper<>());
    }

    private void appendIfPresent(StringBuilder sb, String label, String value) {
        if (value != null && !value.isBlank()) {
            sb.append(label).append("：").append(value).append('\n');
        }
    }

    private void addLine(List<String> lines, String label, String value) {
        if (value != null && !value.isBlank()) {
            lines.add(label + "：" + value);
        }
    }

    private String dateText(java.time.LocalDate date) {
        return date == null ? "至今" : date.toString();
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    /** 个人简介第一段（通常包含出生、学历、毕业时间、所在地、求职意向） */
    private String firstParagraph(Profile profile) {
        if (profile == null || profile.getAbout() == null) {
            return "";
        }
        String about = profile.getAbout().trim();
        int lineBreak = about.indexOf('\n');
        return lineBreak >= 0 ? about.substring(0, lineBreak).trim() : about;
    }
}