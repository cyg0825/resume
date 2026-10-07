package com.example.resume.controller;

import com.example.resume.common.BusinessException;
import com.example.resume.common.Result;
import com.example.resume.common.SimpleRateLimiter;
import com.example.resume.dto.ShareLinkCreateRequest;
import com.example.resume.dto.ShareTokenUpdateRequest;
import com.example.resume.entity.ShareLink;
import com.example.resume.service.ShareLinkService;
import com.example.resume.util.IpUtils;
import com.example.resume.vo.ShareAccessVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 专属分享链接：
 * - GET  /api/share/access            访客凭 token 进入（公开，IP 限流，防爆破）
 * - 管理接口 /api/admin/share-links   生成 / 列表 / 吊销 / 修复 Token（需管理员 JWT）
 */
@Tag(name = "15-专属分享链接", description = "凭链接 token 访问简历；后台生成、查看访问统计、吊销")
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ShareLinkController {

    private final ShareLinkService shareLinkService;
    private final SimpleRateLimiter rateLimiter;

    @Operation(summary = "凭专属链接访问【公开】",
            description = "校验链接有效性并计数，返回访客令牌（有效期取 app.jwt.share-expire，默认 60 天）；"
                    + "同一 IP 每分钟最多 20 次")
    @GetMapping("/share/access")
    public Result<ShareAccessVO> access(@RequestParam String token,
                                        HttpServletRequest request) {
        String ip = IpUtils.getClientIp(request);
        if (!rateLimiter.allow("share-access:" + ip)) {
            throw new BusinessException(429, "访问过于频繁，请稍后再试");
        }
        return Result.success(shareLinkService.accessByToken(token, ip));
    }

    @Operation(summary = "分享链接列表【JWT】")
    @GetMapping("/admin/share-links")
    public Result<List<ShareLink>> list() {
        return Result.success(shareLinkService.list());
    }

    @Operation(summary = "生成分享链接【JWT】")
    @PostMapping("/admin/share-links")
    public Result<ShareLink> create(@Valid @RequestBody ShareLinkCreateRequest request) {
        return Result.success(shareLinkService.create(request));
    }

    @Operation(summary = "吊销分享链接【JWT】")
    @PutMapping("/admin/share-links/{id}/disable")
    public Result<Void> disable(@PathVariable Long id) {
        shareLinkService.disable(id);
        return Result.success();
    }

    @Operation(summary = "修复/更换 Token【JWT】",
            description = "把指定链接的 Token 改成给定值，用于链接被误删后要沿用原链接、或换库迁移后地址不变；"
                    + "取值必须是 64 位十六进制且未被其他链接占用，备注/版本绑定/访问次数不受影响")
    @PutMapping("/admin/share-links/{id}/token")
    public Result<ShareLink> updateToken(@PathVariable Long id,
                                         @Valid @RequestBody ShareTokenUpdateRequest request) {
        return Result.success(shareLinkService.updateToken(id, request.getToken()));
    }
}
