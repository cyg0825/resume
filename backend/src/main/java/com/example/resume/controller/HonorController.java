package com.example.resume.controller;

import com.example.resume.common.Result;
import com.example.resume.entity.Honor;
import com.example.resume.service.HonorService;
import com.example.resume.service.VersionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "16-荣誉证书", description = "荣誉证书按简历主人姓名归档，与简历版本无关；同名版本共享，改名即新人，删版本不删荣誉")
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class HonorController {

    private final HonorService honorService;
    private final VersionService versionService;

    @Operation(summary = "荣誉证书列表【公开，前台按版本对应姓名展示】")
    @GetMapping("/honors")
    public Result<List<Honor>> list(
            @Parameter(description = "简历版本 ID，不传使用默认版本")
            @RequestParam(required = false) Long versionId) {
        return Result.success(
                honorService.listByVersion(versionService.resolveVersionId(versionId)));
    }

    @Operation(summary = "某归属人的荣誉证书【JWT】")
    @GetMapping("/admin/honors")
    public Result<List<Honor>> adminList(
            @Parameter(description = "归属人姓名") @RequestParam String ownerName) {
        return Result.success(honorService.listByOwner(ownerName));
    }

    @Operation(summary = "新增荣誉证书【JWT】", description = "请求体需包含 ownerName（归属人姓名）")
    @PostMapping("/admin/honors")
    public Result<Honor> create(@RequestBody Honor honor) {
        return Result.success(honorService.create(honor));
    }

    @Operation(summary = "修改荣誉证书【JWT】")
    @PutMapping("/admin/honors")
    public Result<Void> update(@RequestBody Honor honor) {
        honorService.update(honor);
        return Result.success();
    }

    @Operation(summary = "删除荣誉证书【JWT】")
    @DeleteMapping("/admin/honors/{id}")
    public Result<Void> delete(
            @Parameter(description = "荣誉证书 ID") @PathVariable Long id) {
        honorService.delete(id);
        return Result.success();
    }
}
