package com.example.resume.controller;

import com.example.resume.common.Result;
import com.example.resume.service.OwnerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 归属人候选（作品集、荣誉证书等按人管理模块共用，需 JWT）
 */
@Tag(name = "00-归属人", description = "按人管理模块共用的归属人姓名候选列表")
@RestController
@RequestMapping("/api/admin/owners")
@RequiredArgsConstructor
public class OwnerController {

    private final OwnerService ownerService;

    @Operation(summary = "归属人姓名列表【JWT】")
    @GetMapping
    public Result<List<String>> list() {
        return Result.success(ownerService.listOwners());
    }
}
