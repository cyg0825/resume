package com.example.resume.controller;

import com.example.resume.common.Result;
import com.example.resume.dto.VisitRequest;
import com.example.resume.service.VisitService;
import com.example.resume.util.IpUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 访客访问上报（公开接口，前端进入页面时调用一次）
 */
@Tag(name = "12-访问上报", description = "前台进入页面时上报访问，用于访客统计")
@RestController
@RequestMapping("/api/visit")
@RequiredArgsConstructor
public class VisitController {

    private final VisitService visitService;

    @Operation(summary = "上报一次访问【公开】",
            description = "IP、UA、来源 Referer 由服务端从请求中获取，前端只需传 path 与 sessionId")
    @PostMapping
    public Result<Void> record(@RequestBody(required = false) VisitRequest body,
                               HttpServletRequest request) {
        String path = body != null ? body.getPath() : null;
        String sessionId = body != null ? body.getSessionId() : null;
        visitService.record(
                IpUtils.getClientIp(request),
                request.getHeader("User-Agent"),
                request.getHeader("Referer"),
                path,
                sessionId);
        return Result.success();
    }
}
