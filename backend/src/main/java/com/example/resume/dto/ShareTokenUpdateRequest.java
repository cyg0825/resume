package com.example.resume.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 后台修复专属链接 Token 请求
 */
@Data
public class ShareTokenUpdateRequest {

    @NotBlank(message = "请填写 Token")
    @Pattern(regexp = "^[0-9a-fA-F]{64}$", message = "Token 需为 64 位十六进制字符串")
    private String token;
}
