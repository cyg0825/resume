package com.example.resume.controller;

import com.example.resume.common.Result;
import com.example.resume.service.VisitService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 后台数据统计面板接口（需 JWT）：
 * 访问总览、访问趋势、来源分布、活跃 IP
 */
@Tag(name = "13-访问统计面板", description = "访问总览/趋势/来源/活跃 IP，全部需 JWT")
@RestController
@RequestMapping("/api/admin/stats")
@RequiredArgsConstructor
public class StatsController {

    private final VisitService visitService;

    @Operation(summary = "总览数据【JWT】",
            description = "累计访问量、独立 IP 数、今日访问/独立访客")
    @GetMapping("/overview")
    public Result<Map<String, Object>> overview() {
        return Result.success(visitService.overview());
    }

    @Operation(summary = "近 N 天访问趋势【JWT】",
            description = "返回每日访问次数与独立访客数（自动补全无访问的日期），N 范围 1-90，默认 7")
    @GetMapping("/trend")
    public Result<List<Map<String, Object>>> trend(
            @Parameter(description = "统计天数，1-90，默认 7")
            @RequestParam(defaultValue = "7") int days) {
        int safeDays = Math.min(Math.max(days, 1), 90);
        return Result.success(visitService.trend(safeDays));
    }

    @Operation(summary = "访问来源分布【JWT】",
            description = "按 Referer 归一化域名分组统计，无 Referer 记为“直接访问”")
    @GetMapping("/sources")
    public Result<List<Map<String, Object>>> sources() {
        return Result.success(visitService.sources());
    }

    @Operation(summary = "活跃 IP Top10【JWT】", description = "近 N 天访问次数最多的 10 个 IP")
    @GetMapping("/top-ips")
    public Result<List<Map<String, Object>>> topIps(
            @Parameter(description = "统计天数，1-90，默认 7")
            @RequestParam(defaultValue = "7") int days) {
        int safeDays = Math.min(Math.max(days, 1), 90);
        return Result.success(visitService.topIps(safeDays));
    }
}
