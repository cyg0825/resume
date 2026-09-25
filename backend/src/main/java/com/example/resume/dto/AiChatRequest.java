package com.example.resume.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "AI 问答请求")
public class AiChatRequest {

    @Schema(description = "会话 ID，由前端生成并持久化，用于串联多轮对话上下文；不传则由服务端生成",
            example = "1758360000000-ab12cd34")
    private String sessionId;

    @Schema(description = "访客提问内容", example = "他的技能栈有哪些？",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "问题不能为空")
    private String question;

    @Schema(description = "基于哪个简历版本回答，不传使用默认版本", example = "1")
    private Long versionId;
}
