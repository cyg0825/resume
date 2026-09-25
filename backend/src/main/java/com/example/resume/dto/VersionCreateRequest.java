package com.example.resume.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 创建简历版本：
 * sourceVersionId 不为空时，把源版本的全部内容复制一份到新版本
 */
@Data
@Schema(description = "创建简历版本请求（可选择从已有版本克隆内容）")
public class VersionCreateRequest {

    @Schema(description = "版本名称", example = "前端开发版",
            requiredMode = Schema.RequiredMode.REQUIRED, maxLength = 100)
    @NotBlank(message = "版本名称不能为空")
    private String versionName;

    @Schema(description = "版本说明", example = "面向前端岗位定制的简历版本")
    private String description;

    @Schema(description = "复制内容的源版本 ID；不传则创建仅含空白个人信息的新版本",
            example = "1")
    private Long sourceVersionId;
}
