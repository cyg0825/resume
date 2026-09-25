package com.example.resume.security;

/**
 * 访客专属链接的认证主体（存入 SecurityContext 的 principal）。
 *
 * @param linkId    专属链接 ID
 * @param versionId 链接绑定的简历版本 ID；null 表示链接未绑定版本（跟随当前默认版本）
 */
public record SharePrincipal(Long linkId, Long versionId) {
}
