package com.example.resume.service;

import com.example.resume.common.BusinessException;
import com.example.resume.entity.*;
import com.example.resume.mapper.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * PDF 简历导入：
 * PDFBox 抽取文本 → 大模型按简历库表结构输出 JSON → 创建新简历版本并写入全部内容。
 * 导入始终生成新版本，不覆盖任何现有版本，导入后可在后台校对并设为默认。
 */
@Slf4j
@Service
public class ResumeImportService {

    private static final Pattern YEAR_MONTH = Pattern.compile("(\\d{4})\\D{0,2}(\\d{1,2})?");

    private final ResumeVersionMapper versionMapper;
    private final ProfileMapper profileMapper;
    private final EducationMapper educationMapper;
    private final ExperienceMapper experienceMapper;
    private final SkillMapper skillMapper;
    private final HonorMapper honorMapper;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    @Value("${app.ai.api-url}")
    private String apiUrl;

    @Value("${app.ai.api-key}")
    private String apiKey;

    @Value("${app.ai.import-model:}")
    private String model;

    public ResumeImportService(ResumeVersionMapper versionMapper,
                               ProfileMapper profileMapper,
                               EducationMapper educationMapper,
                               ExperienceMapper experienceMapper,
                               SkillMapper skillMapper,
                               HonorMapper honorMapper) {
        this.versionMapper = versionMapper;
        this.profileMapper = profileMapper;
        this.educationMapper = educationMapper;
        this.experienceMapper = experienceMapper;
        this.skillMapper = skillMapper;
        this.honorMapper = honorMapper;
    }

    /** 导入结果摘要（返回给前端展示） */
    public record ImportResult(Long versionId, String versionName, Map<String, Integer> counts) {
    }

    @Transactional
    public ImportResult importPdf(byte[] pdfBytes, String originalFilename) {
        if (pdfBytes == null || pdfBytes.length == 0) {
            throw new BusinessException(400, "上传的 PDF 文件为空");
        }
        String rawText = extractText(pdfBytes);
        if (rawText.isBlank()) {
            throw new BusinessException(400, "PDF 中未抽取到文本，可能是纯图片/扫描版简历，请先 OCR 或改用文字版 PDF");
        }
        log.info("PDF 导入：{} 抽取到 {} 字符", originalFilename, rawText.length());

        JsonNode root = parseStructuredJson(callLlm(rawText));

        // 1. 创建新版本（非默认，需用户校对后手动设为默认）
        LocalDateTime now = LocalDateTime.now();
        String stamp = now.format(DateTimeFormatter.ofPattern("MM-dd HH:mm"));
        ResumeVersion version = new ResumeVersion();
        version.setVersionName("PDF导入-" + stamp);
        version.setDescription("由 PDF 文件「" + originalFilename + "」导入，请校对内容");
        version.setIsDefault(null);
        versionMapper.insert(version);
        Long vid = version.getId();

        Map<String, Integer> counts = new LinkedHashMap<>();

        // 2. 个人信息
        JsonNode profileNode = root.path("profile");
        Profile profile = new Profile();
        profile.setVersionId(vid);
        profile.setName(text(profileNode, "name"));
        profile.setJobTitle(text(profileNode, "jobTitle"));
        profile.setSlogan(text(profileNode, "slogan"));
        profile.setEmail(emptyToNull(text(profileNode, "email")));
        profile.setPhone(emptyToNull(text(profileNode, "phone")));
        profile.setGithub(emptyToNull(text(profileNode, "github")));
        profile.setGitee(emptyToNull(text(profileNode, "gitee")));
        profile.setCsdn(emptyToNull(text(profileNode, "csdn")));
        profile.setAddress(emptyToNull(text(profileNode, "address")));
        profile.setAbout(emptyToNull(text(profileNode, "about")));
        profileMapper.insert(profile);

        // 3. 教育经历
        int eduSort = 0;
        for (JsonNode n : root.path("education")) {
            Education e = new Education();
            e.setVersionId(vid);
            e.setSchool(text(n, "school"));
            e.setMajor(text(n, "major"));
            e.setDegree(emptyToNull(text(n, "degree")));
            e.setStartDate(parseDate(text(n, "startDate")));
            e.setEndDate(parseDate(text(n, "endDate")));
            e.setDescription(emptyToNull(text(n, "description")));
            e.setSort(++eduSort);
            if (notBlank(e.getSchool()) || notBlank(e.getMajor())) {
                educationMapper.insert(e);
            }
        }
        counts.put("education", eduSort);

        // 4. 工作/实习经历（type=1）
        int workSort = 0;
        for (JsonNode n : root.path("workExperiences")) {
            Experience e = new Experience();
            e.setVersionId(vid);
            e.setType(1);
            e.setCompany(text(n, "company"));
            e.setPosition(emptyToNull(text(n, "position")));
            e.setStartDate(parseDate(text(n, "startDate")));
            e.setEndDate(parseDate(text(n, "endDate")));
            e.setDescription(emptyToNull(joinBullets(n)));
            e.setSort(++workSort);
            if (notBlank(e.getCompany()) || notBlank(e.getPosition())) {
                experienceMapper.insert(e);
            }
        }
        counts.put("workExperience", workSort);

        // 5. 项目经历（type=2）
        int projSort = 0;
        for (JsonNode n : root.path("projects")) {
            Experience e = new Experience();
            e.setVersionId(vid);
            e.setType(2);
            e.setCompany(text(n, "name"));
            e.setPosition(emptyToNull(text(n, "role")));
            e.setStartDate(parseDate(text(n, "startDate")));
            e.setEndDate(parseDate(text(n, "endDate")));
            String desc = text(n, "description");
            String highlights = joinArray(n.path("highlights"));
            e.setDescription(emptyToNull(highlights.isBlank() ? desc : desc + "\n" + highlights));
            e.setTechStack(emptyToNull(joinArray(n.path("techStack"))));
            e.setSort(++projSort);
            if (notBlank(e.getCompany()) || notBlank(e.getDescription())) {
                experienceMapper.insert(e);
            }
        }
        counts.put("projectExperience", projSort);

        // 6. 技能
        int skillSort = 0;
        for (JsonNode n : root.path("skills")) {
            Skill s = new Skill();
            s.setVersionId(vid);
            s.setCategory(emptyToNull(text(n, "category")));
            s.setName(text(n, "name"));
            int level = n.path("level").asInt(75);
            s.setLevel(Math.max(0, Math.min(100, level)));
            s.setSort(++skillSort);
            if (notBlank(s.getName())) {
                skillMapper.insert(s);
            }
        }
        counts.put("skill", skillSort);

        // 7. 荣誉证书（按简历主人姓名归档，归属本次导入版本的姓名；姓名缺失则跳过）
        int honorSort = 0;
        String ownerName = profile.getName();
        if (notBlank(ownerName)) {
            for (JsonNode n : root.path("honors")) {
                Honor h = new Honor();
                h.setOwnerName(ownerName.trim());
                h.setTitle(text(n, "title"));
                h.setIssuer(emptyToNull(text(n, "issuer")));
                h.setLevel(emptyToNull(text(n, "level")));
                h.setHonorDate(parseDate(text(n, "honorDate")));
                h.setDescription(emptyToNull(text(n, "description")));
                h.setSort(++honorSort);
                if (notBlank(h.getTitle())) {
                    honorMapper.insert(h);
                }
            }
        }
        counts.put("honor", honorSort);

        log.info("PDF 导入完成，新版本 id={}，内容统计={}", vid, counts);
        return new ImportResult(vid, version.getVersionName(), counts);
    }

    // ==================== PDF 文本抽取 ====================

    private String extractText(byte[] bytes) {
        try (PDDocument doc = Loader.loadPDF(bytes)) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            return stripper.getText(doc).trim();
        } catch (Exception e) {
            log.warn("PDF 文本抽取失败", e);
            throw new BusinessException(400, "PDF 解析失败：" + e.getMessage());
        }
    }

    // ==================== 大模型结构化 ====================

    private String callLlm(String resumeText) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new BusinessException(500, "尚未配置大模型 API Key，无法解析 PDF（请在后端配置 app.ai.api-key）");
        }

        String systemPrompt = """
                你是简历结构化引擎。把用户提供的简历纯文本抽取为严格符合下面 JSON Schema 的对象，
                只输出 JSON，不要输出任何解释或 markdown 代码块。
                规则：
                1) 中文内容保持原文，不要翻译、不要补充简历中没有的信息；
                2) 所有日期统一为 "YYYY-MM" 格式（如 2024-09），结束时间为“至今/现在/present”时填 "至今"，无法判断则留空；
                3) workExperiences 只收录工作/实习经历，description 把职责与业绩整理为多条要点，每条一行；
                4) projects 收录软件/课程/竞赛项目，techStack 为技术名词数组，highlights 为亮点数组；
                5) skills 按类别分组（如 测试工具/编程语言/数据库/前端/后端），level 按简历描述推断 60-95；
                6) honors.level 取 国家级/省级/校级/其他 之一；
                7) 简历中没有的字段一律留空字符串，对应数组没有内容则给空数组；
                8) 链接字段只填完整 URL，不要填平台名称。
                Schema：
                {
                  "profile": {"name":"","jobTitle":"","slogan":"","email":"","phone":"","github":"","gitee":"","csdn":"","address":"","about":""},
                  "education": [{"school":"","major":"","degree":"","startDate":"","endDate":"","description":""}],
                  "workExperiences": [{"company":"","position":"","startDate":"","endDate":"","description":"每行一条要点"}],
                  "projects": [{"name":"","role":"","startDate":"","endDate":"","description":"","techStack":[""],"highlights":[""]}],
                  "skills": [{"category":"","name":"","level":80}],
                  "honors": [{"title":"","issuer":"","level":"","honorDate":"","description":""}]
                }""";

        try {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("model", model);
            body.put("temperature", 0.1);
            // 内容丰富的多页简历结构化 JSON 可能达 6000+ token，给 8192 防止截断
            body.put("max_tokens", 8192);
            body.put("messages", java.util.List.of(
                    Map.of("role", "system", "content", systemPrompt),
                    Map.of("role", "user", "content", resumeText)
            ));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl))
                    // 结构化生成耗时明显长于普通问答，独立给 120 秒
                    .timeout(Duration.ofSeconds(120))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(
                            objectMapper.writeValueAsString(body), StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new BusinessException(500, "大模型服务返回 " + response.statusCode() + "，请稍后重试");
            }
            JsonNode resp = objectMapper.readTree(response.body());
            String content = resp.path("choices").path(0).path("message").path("content").asText("");
            if (content.isBlank()) {
                throw new BusinessException(500, "大模型未返回结构化结果，请稍后重试");
            }
            return content;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.warn("PDF 结构化调用失败", e);
            throw new BusinessException(500, "PDF 解析服务调用失败：" + e.getMessage());
        }
    }

    /** 容错解析：剥除 ```json 围栏，截取首个 { 到末个 } */
    private JsonNode parseStructuredJson(String text) {
        String t = text.trim();
        if (t.startsWith("```")) {
            int first = t.indexOf('\n');
            int last = t.lastIndexOf("```");
            if (first > 0 && last > first) {
                t = t.substring(first + 1, last).trim();
            }
        }
        int brace = t.indexOf('{');
        int close = t.lastIndexOf('}');
        if (brace >= 0 && close > brace) {
            t = t.substring(brace, close + 1);
        }
        try {
            JsonNode node = objectMapper.readTree(t);
            if (!node.path("profile").isObject()) {
                throw new BusinessException(500, "结构化结果缺少 profile 字段，请更换 PDF 后重试");
            }
            return node;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.warn("结构化 JSON 解析失败: {}\n原文片段: {}", e.getMessage(),
                    text.substring(0, Math.min(300, text.length())));
            throw new BusinessException(500, "大模型返回内容无法解析为简历数据，请重试或更换 PDF");
        }
    }

    // ==================== 工具方法 ====================

    private String text(JsonNode node, String field) {
        JsonNode v = node.path(field);
        if (v.isMissingNode() || v.isNull()) {
            return "";
        }
        return v.asText("").trim();
    }

    private String emptyToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    private boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }

    /** 数组拼成换行/逗号文本 */
    private String joinArray(JsonNode array) {
        if (array == null || !array.isArray() || array.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (JsonNode item : array) {
            String s = item.asText("").trim();
            if (!s.isEmpty()) {
                if (sb.length() > 0) {
                    sb.append('\n');
                }
                sb.append(s);
            }
        }
        return sb.toString();
    }

    /** description 可能是数组（要点）或字符串，统一为每行一条 */
    private String joinBullets(JsonNode node) {
        JsonNode desc = node.path("description");
        if (desc.isArray()) {
            StringBuilder sb = new StringBuilder();
            for (JsonNode item : desc) {
                String s = item.asText("").trim();
                if (!s.isEmpty()) {
                    if (sb.length() > 0) {
                        sb.append('\n');
                    }
                    sb.append(s);
                }
            }
            return sb.toString();
        }
        return desc.asText("").trim();
    }

    /**
     * 解析 "YYYY-MM" / "YYYY.MM" / "YYYY/M" / "YYYY年M月" / "YYYY-MM-DD" 等；
     * 空值或“至今/present/now”返回 null（表示至今）。
     */
    private LocalDate parseDate(String value) {
        if (value == null) {
            return null;
        }
        String v = value.trim().toLowerCase();
        if (v.isEmpty() || v.contains("至今") || v.contains("现在") || v.contains("目前")
                || v.contains("present") || v.contains("current") || v.contains("now")) {
            return null;
        }
        Matcher m = YEAR_MONTH.matcher(v);
        if (m.find()) {
            int year = Integer.parseInt(m.group(1));
            int month = m.group(2) != null ? Integer.parseInt(m.group(2)) : 1;
            month = Math.max(1, Math.min(12, month));
            return LocalDate.of(year, month, 1);
        }
        return null;
    }
}
