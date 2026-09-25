package com.example.resume.security;

import com.example.resume.entity.ShareLink;
import com.example.resume.mapper.ShareLinkMapper;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

/**
 * JWT 认证过滤器：从 Authorization 头解析 Bearer token，
 * 校验通过后写入 SecurityContext。
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final ShareLinkMapper shareLinkMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (StringUtils.hasText(header) && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            Claims claims = jwtUtil.parse(token);
            if (claims != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                // 访客令牌：每次请求回查链接状态，保证吊销/过期立即生效
                if ("share".equals(claims.get("kind")) && !isLinkStillValid(claims)) {
                    filterChain.doFilter(request, response);
                    return;
                }
                String username = claims.getSubject();
                // 访客专属链接令牌只有只读权限，管理员令牌拥有全部权限
                boolean visitor = "share".equals(claims.get("kind"));
                String role = visitor ? "ROLE_VISITOR" : "ROLE_ADMIN";
                // 访客主体携带链接绑定版本（claim 不存在 = 链接未绑定版本，跟随默认），
                // 供 VersionService 做服务端版本隔离，防止访客篡改 versionId 越权读取其他版本
                Object principal = username;
                if (visitor) {
                    Object vidObj = claims.get("versionId");
                    Long boundVersionId = vidObj instanceof Number n ? n.longValue() : null;
                    Object linkIdObj = claims.get("linkId");
                    Long linkId = linkIdObj instanceof Number n ? n.longValue() : null;
                    principal = new SharePrincipal(linkId, boundVersionId);
                }
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                principal, null,
                                List.of(new SimpleGrantedAuthority(role)));
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        }
        filterChain.doFilter(request, response);
    }

    /**
     * 校验访客令牌对应的分享链接：仍启用且未过期
     */
    private boolean isLinkStillValid(Claims claims) {
        try {
            Object linkIdObj = claims.get("linkId");
            if (linkIdObj == null) {
                return false;
            }
            ShareLink link = shareLinkMapper.selectById(((Number) linkIdObj).longValue());
            return link != null
                    && Integer.valueOf(1).equals(link.getEnabled())
                    && (link.getExpireTime() == null
                    || link.getExpireTime().isAfter(LocalDateTime.now()));
        } catch (Exception e) {
            return false;
        }
    }
}
