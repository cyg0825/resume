package com.example.resume.controller;

import com.example.resume.common.Result;
import com.example.resume.entity.Experience;
import com.example.resume.service.ExperienceService;
import com.example.resume.service.VersionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "04-工作/项目经历", description = "工作经历(type=1)与项目经历(type=2)的查询及后台维护")
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ExperienceController {

    private final ExperienceService experienceService;
    private final VersionService versionService;

    @Operation(summary = "工作/项目经历列表【公开】",
            description = "type=1 工作经历，type=2 项目经历；不传 type 返回全部")
    @GetMapping("/experiences")
    public Result<List<Experience>> list(
            @Parameter(description = "简历版本 ID，不传使用默认版本")
            @RequestParam(required = false) Long versionId,
            @Parameter(description = "经历类型：1=工作经历 2=项目经历")
            @RequestParam(required = false) Integer type) {
        return Result.success(experienceService.listByVersion(
                versionService.resolveVersionId(versionId), type));
    }

    @Operation(summary = "新增经历【JWT】")
    @PostMapping("/admin/experiences")
    public Result<Void> create(@RequestBody Experience experience) {
        experienceService.create(experience);
        return Result.success();
    }

    @Operation(summary = "修改经历【JWT】")
    @PutMapping("/admin/experiences")
    public Result<Void> update(@RequestBody Experience experience) {
        experienceService.update(experience);
        return Result.success();
    }

    @Operation(summary = "删除经历【JWT】")
    @DeleteMapping("/admin/experiences/{id}")
    public Result<Void> delete(
            @Parameter(description = "经历 ID") @PathVariable Long id) {
        experienceService.delete(id);
        return Result.success();
    }
}
