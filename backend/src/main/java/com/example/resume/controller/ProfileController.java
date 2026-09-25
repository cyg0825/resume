package com.example.resume.controller;

import com.example.resume.common.Result;
import com.example.resume.entity.Profile;
import com.example.resume.service.ProfileService;
import com.example.resume.service.VersionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "02-个人信息", description = "个人信息的查询与后台维护")
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;
    private final VersionService versionService;

    @Operation(summary = "获取个人信息【公开】",
            description = "通过 versionId 指定简历版本，不传则返回后台设置的默认版本")
    @GetMapping("/profile")
    public Result<Profile> getProfile(
            @Parameter(description = "简历版本 ID，不传使用默认版本")
            @RequestParam(required = false) Long versionId) {
        return Result.success(profileService.getByVersion(versionService.resolveVersionId(versionId)));
    }

    @Operation(summary = "更新个人信息【JWT】",
            description = "按 versionId 新增或更新该版本的个人信息（每个版本仅一条）")
    @PutMapping("/admin/profile")
    public Result<Profile> updateProfile(@RequestBody Profile profile) {
        if (profile.getVersionId() == null) {
            profile.setVersionId(versionService.resolveVersionId(null));
        }
        return Result.success(profileService.saveByVersion(profile));
    }
}
