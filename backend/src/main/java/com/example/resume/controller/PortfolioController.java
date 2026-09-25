package com.example.resume.controller;

import com.example.resume.common.Result;
import com.example.resume.entity.Portfolio;
import com.example.resume.service.PortfolioService;
import com.example.resume.service.VersionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "06-作品集", description = "作品集按简历主人姓名归属，与简历版本无关；同名版本共享，改名即新人，删版本不删作品")
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class PortfolioController {

    private final PortfolioService portfolioService;
    private final VersionService versionService;

    @Operation(summary = "作品集列表【公开，前台按版本对应姓名展示】")
    @GetMapping("/portfolios")
    public Result<List<Portfolio>> list(
            @Parameter(description = "简历版本 ID，不传使用默认版本")
            @RequestParam(required = false) Long versionId) {
        return Result.success(
                portfolioService.listByVersion(versionService.resolveVersionId(versionId)));
    }

    @Operation(summary = "归属人姓名列表【JWT】",
            description = "个人信息中出现过的姓名与已拥有作品的姓名并集，用于后台按人管理")
    @GetMapping("/admin/portfolios/owners")
    public Result<List<String>> owners() {
        return Result.success(portfolioService.listOwners());
    }

    @Operation(summary = "某归属人的作品集【JWT】")
    @GetMapping("/admin/portfolios")
    public Result<List<Portfolio>> adminList(
            @Parameter(description = "归属人姓名") @RequestParam String ownerName) {
        return Result.success(portfolioService.listByOwner(ownerName));
    }

    @Operation(summary = "新增作品【JWT】", description = "请求体需包含 ownerName（归属人姓名）")
    @PostMapping("/admin/portfolios")
    public Result<Void> create(@RequestBody Portfolio portfolio) {
        portfolioService.create(portfolio);
        return Result.success();
    }

    @Operation(summary = "修改作品【JWT】")
    @PutMapping("/admin/portfolios")
    public Result<Void> update(@RequestBody Portfolio portfolio) {
        portfolioService.update(portfolio);
        return Result.success();
    }

    @Operation(summary = "删除作品【JWT】")
    @DeleteMapping("/admin/portfolios/{id}")
    public Result<Void> delete(
            @Parameter(description = "作品 ID") @PathVariable Long id) {
        portfolioService.delete(id);
        return Result.success();
    }
}
