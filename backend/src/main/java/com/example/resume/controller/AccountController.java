package com.example.resume.controller;

import com.example.resume.common.Result;
import com.example.resume.dto.PasswordChangeRequest;
import com.example.resume.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理员账号：改密入口落在 /api/admin/** 之下，
 * 由 SecurityConfig 的 hasRole("ADMIN") 统一收口，不再往公开前缀 /api/auth 上加接口。
 */
@Tag(name = "18-账号安全", description = "管理员修改登录密码")
@RestController
@RequestMapping("/api/admin/account")
@RequiredArgsConstructor
public class AccountController {

    private final AuthService authService;

    @Operation(summary = "修改密码【JWT】",
            description = "校验原密码后重新 BCrypt 存储；只影响后续登录，已签发的 JWT 不失效")
    @PutMapping("/password")
    public Result<Void> changePassword(Authentication authentication,
                                       @Valid @RequestBody PasswordChangeRequest request) {
        authService.changePassword(authentication.getName(), request);
        return Result.success();
    }
}
