package com.example.resume.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 访客访问日志：每次访问一条，通过 DISTINCT ip 实现独立 IP 去重统计
 */
@Data
@TableName("visit_log")
public class VisitLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 客户端真实 IP */
    private String ip;

    private String userAgent;

    /** 访问来源（Referer） */
    private String referer;

    /** 访问页面路径 */
    private String path;

    /** 浏览器会话标识（localStorage 生成，辅助区分同 IP 多访客） */
    private String sessionId;

    private LocalDateTime visitTime;
}
