package com.example.resume.controller;

import com.example.resume.common.Result;
import com.example.resume.service.FileStorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * 文件上传（图片，需 JWT）
 * 返回 { "url": "/uploads/2026/09/xxx.png" }
 */
@Tag(name = "15-文件上传", description = "图片上传，按年月分目录存储，返回可访问 URL（需 JWT）")
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class UploadController {

    private final FileStorageService fileStorageService;

    @Operation(summary = "上传图片【JWT】",
            description = "multipart/form-data 提交，字段名 file；"
                    + "支持 jpg/jpeg/png/gif/webp/svg/bmp，单文件最大 10MB")
    @PostMapping("/upload")
    public Result<Map<String, String>> upload(
            @RequestParam("file") MultipartFile file) {
        String url = fileStorageService.storeImage(file);
        return Result.success(Map.of("url", url));
    }
}
