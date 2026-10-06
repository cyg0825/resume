package com.example.resume.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT 工具：签发、解析、校验
 */
@Slf4j
@Component
public class JwtUtil {

    @Value("${app.jwt.secret}")
    private String secret;

    @Value("${app.jwt.expire}")
    private long expire;

    /** 访客专属链接访问令牌有效期（毫秒），由 app.jwt.share-expire 配置，默认 60 天 */
    @Value("${app.jwt.share-expire}")
    private long shareExpire;

    private SecretKey key;

    @PostConstruct
    public void init() {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 生成 token
     */
    public String generateToken(Long userId, String username) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expire);
        return Jwts.builder()
                .subject(username)
                .claim("userId", userId)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(key)
                .compact();
    }

    /**
     * 生成访客专属链接访问令牌（与管理员令牌区分：kind=share）
     *
     * @param linkId    分享链接 ID
     * @param versionId 链接绑定的简历版本（可为 null）
     */
    public String generateShareToken(Long linkId, Long versionId) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + shareExpire);
        var builder = Jwts.builder()
                .subject("share-" + linkId)
                .claim("kind", "share")
                .claim("linkId", linkId)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(key);
        if (versionId != null) {
            builder.claim("versionId", versionId);
        }
        return builder.compact();
    }

    /**
     * 解析 token，失败返回 null
     */
    public Claims parse(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (Exception e) {
            log.debug("JWT 解析失败: {}", e.getMessage());
            return null;
        }
    }
}
