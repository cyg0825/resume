package com.example.resume.controller;

import com.example.resume.common.Result;
import com.example.resume.dto.VersionCreateRequest;
import com.example.resume.entity.ResumeVersion;
import com.example.resume.service.VersionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 简历版本管理（后台）。
 * 前台通过各公开接口的 versionId 参数选择展示版本，未传时展示默认版本。
 */
@Tag(name = "09-简历版本管理", description = "多套简历版本的创建/克隆/切换默认/删除（全部需 JWT）")
@RestController
@RequestMapping("/api/admin/versions")
@RequiredArgsConstructor
public class VersionController {

    private final VersionService versionService;

    @Operation(summary = "版本列表【JWT】")
    @GetMapping
    public Result<List<ResumeVersion>> list() {
        return Result.success(versionService.list());
    }

    @Operation(summary = "创建版本【JWT】",
            description = "sourceVersionId 不为空时，将源版本的个人信息/教育/经历/技能/作品完整克隆一份")
    @PostMapping
    public Result<ResumeVersion> create(@Valid @RequestBody VersionCreateRequest request) {
        return Result.success(versionService.create(request));
    }

    @Operation(summary = "修改版本名称/说明【JWT】")
    @PutMapping
    public Result<Void> update(@RequestBody ResumeVersion version) {
        versionService.update(version);
        return Result.success();
    }

    @Operation(summary = "一键切换默认版本【JWT】",
            description = "切换后访客首页立即展示该版本内容")
    @PutMapping("/{id}/default")
    public Result<Void> setDefault(@Parameter(description = "要设为默认的版本 ID") @PathVariable Long id) {
        versionService.setDefault(id);
        return Result.success();
    }

    @Operation(summary = "删除版本【JWT】",
            description = "会级联删除该版本下所有简历内容；默认展示版本禁止删除")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@Parameter(description = "版本 ID") @PathVariable Long id) {
        versionService.delete(id);
        return Result.success();
    }
}
