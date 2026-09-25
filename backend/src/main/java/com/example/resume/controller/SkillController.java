package com.example.resume.controller;

import com.example.resume.common.Result;
import com.example.resume.entity.Skill;
import com.example.resume.service.SkillService;
import com.example.resume.service.VersionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "05-技能特长", description = "技能列表查询与后台增删改")
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class SkillController {

    private final SkillService skillService;
    private final VersionService versionService;

    @Operation(summary = "技能列表【公开】")
    @GetMapping("/skills")
    public Result<List<Skill>> list(
            @Parameter(description = "简历版本 ID，不传使用默认版本")
            @RequestParam(required = false) Long versionId) {
        return Result.success(
                skillService.listByVersion(versionService.resolveVersionId(versionId)));
    }

    @Operation(summary = "新增技能【JWT】", description = "level 为熟练度 0-100")
    @PostMapping("/admin/skills")
    public Result<Void> create(@RequestBody Skill skill) {
        skillService.create(skill);
        return Result.success();
    }

    @Operation(summary = "修改技能【JWT】")
    @PutMapping("/admin/skills")
    public Result<Void> update(@RequestBody Skill skill) {
        skillService.update(skill);
        return Result.success();
    }

    @Operation(summary = "删除技能【JWT】")
    @DeleteMapping("/admin/skills/{id}")
    public Result<Void> delete(
            @Parameter(description = "技能 ID") @PathVariable Long id) {
        skillService.delete(id);
        return Result.success();
    }
}
