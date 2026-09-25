package com.example.resume.controller;

import com.example.resume.common.Result;
import com.example.resume.dto.LoginRequest;
import com.example.resume.service.AuthService;
import com.example.resume.vo.LoginVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "01-认证管理", description = "管理员登录获取 JWT、退出登录")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "管理员登录", description = "用户名密码校验成功后签发 JWT，默认账号 admin/admin123")
    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody LoginRequest request) {
        return Result.success(authService.login(request));
    }

    @Operation(summary = "退出登录", description = "无状态 JWT 由前端丢弃 token 即可，接口保留用于统一交互")
    @PostMapping("/logout")
    public Result<Void> logout() {
        return Result.success();
    }
}
