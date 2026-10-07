package com.example.resume.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 管理员修改密码请求
 */
@Data
@Schema(description = "管理员修改密码请求")
public class PasswordChangeRequest {

    @Schema(description = "当前密码", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "请填写原密码")
    private String oldPassword;

    @Schema(description = "新密码，8~64 位", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "请填写新密码")
    @Size(min = 8, max = 64, message = "新密码长度需为 8~64 位")
    private String newPassword;
}
