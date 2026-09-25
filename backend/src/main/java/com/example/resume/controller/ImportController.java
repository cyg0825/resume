package com.example.resume.controller;

import com.example.resume.common.BusinessException;
import com.example.resume.common.Result;
import com.example.resume.service.ResumeImportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * PDF 简历导入（需 JWT）
 */
@Tag(name = "16-PDF简历导入", description = "上传 PDF 简历，AI 自动识别为结构化内容并生成新版本（需 JWT）")
@RestController
@RequestMapping("/api/admin/import")
@RequiredArgsConstructor
public class ImportController {

    private final ResumeImportService resumeImportService;

    @Operation(summary = "导入 PDF 简历【JWT】",
            description = "multipart/form-data 提交，字段名 file；AI 识别后创建新简历版本，"
                    + "不覆盖现有内容，单文件最大 10MB")
    @PostMapping("/pdf")
    public Result<ResumeImportService.ImportResult> importPdf(
            @RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(400, "请选择要导入的 PDF 文件");
        }
        String filename = file.getOriginalFilename() != null ? file.getOriginalFilename() : "简历.pdf";
        if (!filename.toLowerCase().endsWith(".pdf")) {
            throw new BusinessException(400, "仅支持 PDF 格式文件");
        }
        try {
            return Result.success(resumeImportService.importPdf(file.getBytes(), filename));
        } catch (java.io.IOException e) {
            throw new BusinessException(500, "文件读取失败：" + e.getMessage());
        }
    }
}
