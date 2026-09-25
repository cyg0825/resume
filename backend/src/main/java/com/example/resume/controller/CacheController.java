package com.example.resume.controller;

import com.example.resume.common.Result;
import com.example.resume.service.AssetVersionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@Tag(name = "17-缓存管理", description = "站点静态资源版本指纹缓存管理；图片URL带文件修改时间指纹，文件替换后自动失效")
@RestController
@RequestMapping("/api/admin/cache")
@RequiredArgsConstructor
public class CacheController {

    private final AssetVersionService assetVersionService;

    @Operation(summary = "一键刷新资源缓存【JWT】",
            description = "清空图片URL版本指纹缓存，强制按文件最新修改时间重新计算；" +
                    "用于宝塔/系统层面直接覆盖图片文件后立即生效")
    @PostMapping("/refresh")
    public Result<Map<String, Object>> refresh() {
        long ts = assetVersionService.refresh();
        String time = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                .withZone(ZoneId.systemDefault())
                .format(Instant.ofEpochMilli(ts));
        return Result.success(Map.of(
                "refreshedAt", time,
                "message", "缓存已刷新，全站图片将按最新文件重新加载"
        ));
    }
}
