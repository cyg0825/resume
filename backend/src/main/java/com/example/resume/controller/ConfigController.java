package com.example.resume.controller;

import com.example.resume.common.Result;
import com.example.resume.entity.SiteConfig;
import com.example.resume.service.SiteConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 站点配置：
 * - 前台 GET /api/config 获取主题默认值、AI 开关等
 * - 后台 PUT /api/admin/config 修改配置（主题默认值在此配置）
 */
@Tag(name = "11-站点配置", description = "站点标题、默认主题与 AI 开关")
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ConfigController {

    private final SiteConfigService siteConfigService;

    @Operation(summary = "获取站点配置【公开】",
            description = "前台据此决定默认主题、是否显示 AI 入口等")
    @GetMapping("/config")
    public Result<SiteConfig> getConfig() {
        return Result.success(siteConfigService.getConfig());
    }

    @Operation(summary = "更新站点配置【JWT】",
            description = "可修改站点标题、默认主题(default/dark/fresh)、AI 问答开关")
    @PutMapping("/admin/config")
    public Result<Void> update(@RequestBody SiteConfig config) {
        siteConfigService.update(config);
        return Result.success();
    }
}