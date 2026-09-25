package com.example.resume.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 前端上报的访问信息（IP、UA 由服务端获取，防止伪造）
 */
@Data
@Schema(description = "访问上报请求，IP/UA/来源由服务端自动获取")
public class VisitRequest {

    @Schema(description = "访问页面路径", example = "/")
    private String path;

    @Schema(description = "浏览器会话标识（前端 localStorage 生成），辅助区分同 IP 多访客",
            example = "1758360000000-ab12cd34")
    private String sessionId;
}
