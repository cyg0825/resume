package com.example.resume.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.resume.common.Result;
import com.example.resume.dto.AiChatRequest;
import com.example.resume.entity.AiChatHistory;
import com.example.resume.service.AiService;
import com.example.resume.service.SiteConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * AI 智能问答：
 * - POST /api/ai/chat 访客提问（公开），答案基于简历知识库
 * - GET/DELETE /api/admin/ai/history 后台问答历史管理（需 JWT）
 */
@Tag(name = "14-AI 智能问答", description = "基于简历知识库的访客问答；后台问答历史管理")
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AiController {

    private final AiService aiService;
    private final SiteConfigService siteConfigService;

    @Operation(summary = "AI 提问【公开】",
            description = "以指定版本(versionId，不传用默认版本)的简历全部内容作为知识库回答；"
                    + "同一 sessionId 自动携带最近 6 轮上下文；未配置大模型时返回本地问答结果(source=local)")
    @PostMapping("/ai/chat")
    public Result<Map<String, Object>> chat(@Valid @RequestBody AiChatRequest request) {
        if (!Integer.valueOf(1).equals(siteConfigService.getConfig().getAiEnabled())) {
            return Result.error(403, "AI 问答功能已关闭");
        }
        return Result.success(aiService.chat(request));
    }

    @Operation(summary = "问答历史分页【JWT】", description = "可按 sessionId 过滤某一次完整会话")
    @GetMapping("/admin/ai/history")
    public Result<Page<AiChatHistory>> history(
            @Parameter(description = "当前页码") @RequestParam(defaultValue = "1") long current,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") long size,
            @Parameter(description = "会话 ID 过滤") @RequestParam(required = false) String sessionId) {
        return Result.success(aiService.adminPage(current, size, sessionId));
    }

    @Operation(summary = "删除单条问答【JWT】")
    @DeleteMapping("/admin/ai/history/{id}")
    public Result<Void> delete(@Parameter(description = "问答记录 ID") @PathVariable Long id) {
        aiService.delete(id);
        return Result.success();
    }

    @Operation(summary = "清空全部问答记录【JWT】")
    @DeleteMapping("/admin/ai/history")
    public Result<Void> clear() {
        aiService.clear();
        return Result.success();
    }
}
