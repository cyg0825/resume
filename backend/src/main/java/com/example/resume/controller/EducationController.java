package com.example.resume.controller;

import com.example.resume.common.Result;
import com.example.resume.entity.Education;
import com.example.resume.service.EducationService;
import com.example.resume.service.VersionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "03-教育经历", description = "教育经历的公开查询与后台增删改")
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class EducationController {

    private final EducationService educationService;
    private final VersionService versionService;

    @Operation(summary = "教育经历列表【公开】")
    @GetMapping("/educations")
    public Result<List<Education>> list(
            @Parameter(description = "简历版本 ID，不传使用默认版本")
            @RequestParam(required = false) Long versionId) {
        return Result.success(
                educationService.listByVersion(versionService.resolveVersionId(versionId)));
    }

    @Operation(summary = "新增教育经历【JWT】")
    @PostMapping("/admin/educations")
    public Result<Void> create(@RequestBody Education education) {
        educationService.create(education);
        return Result.success();
    }

    @Operation(summary = "修改教育经历【JWT】")
    @PutMapping("/admin/educations")
    public Result<Void> update(@RequestBody Education education) {
        educationService.update(education);
        return Result.success();
    }

    @Operation(summary = "删除教育经历【JWT】")
    @DeleteMapping("/admin/educations/{id}")
    public Result<Void> delete(
            @Parameter(description = "教育经历 ID") @PathVariable Long id) {
        educationService.delete(id);
        return Result.success();
    }
}
