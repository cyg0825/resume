package com.example.resume.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * AI 智能问答历史记录
 */
@Data
@TableName("ai_chat_history")
public class AiChatHistory {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 同一会话的标识，用于串联多轮对话 */
    private String sessionId;

    private String question;

    private String answer;

    /** 回答来源：llm 大模型 / local 本地降级 */
    private String source;

    private LocalDateTime createTime;
}
