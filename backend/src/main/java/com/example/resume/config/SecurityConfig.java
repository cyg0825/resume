package com.example.resume.config;

import com.example.resume.common.Result;
import com.example.resume.security.JwtAuthenticationFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.nio.charset.StandardCharsets;

/**
 * Spring Security 配置（专属链接模式）：
 * - 简历内容接口：管理员或持有有效访客令牌（通过专属链接换取）的访客可访问
 * - /api/admin/**：仅管理员
 * - 链接访问接口 /api/share/access、登录、上传的图片资源：公开
 * - 其余请求一律拒绝
 */
@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> {})
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Knife4j / springdoc-openapi 接口文档资源放行
                        .requestMatchers(
                                "/doc.html",
                                "/favicon.ico",
                                "/error",
                                "/webjars/**",
                                "/swagger-ui/**",
                                "/swagger-resources/**",
                                "/v3/api-docs/**"
                        ).permitAll()
                        // 登录、专属链接访问入口：公开
                        .requestMatchers("/api/auth/**", "/api/share/access").permitAll()
                        // 上传的图片/文件：<img> 标签无法携带鉴权头，文件名本身为随机串
                        .requestMatchers("/uploads/**").permitAll()
                        // 简历只读内容：管理员或通过专属链接进入的访客
                        .requestMatchers(HttpMethod.GET,
                                "/api/profile",
                                "/api/educations",
                                "/api/experiences",
                                "/api/skills",
                                "/api/honors",
                                "/api/portfolios",
                                "/api/config"
                        ).hasAnyRole("ADMIN", "VISITOR")
                        // 访客访问上报、AI 问答：管理员或访客
                        .requestMatchers(HttpMethod.POST,
                                "/api/visit",
                                "/api/ai/chat"
                        ).hasAnyRole("ADMIN", "VISITOR")
                        // 后台一切写接口/管理接口：仅管理员
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .anyRequest().denyAll())
                .exceptionHandling(ex -> ex
                        // 未登录/访客凭证失效访问受保护接口返回 401 统一 JSON
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setStatus(401);
                            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
                            response.getWriter().write(objectMapper.writeValueAsString(
                                    Result.error(401, "访问凭证无效或已过期，请通过专属链接重新打开")));
                        })
                        // 已认证但权限不足（访客试图访问管理接口）返回 403
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            response.setStatus(403);
                            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
                            response.getWriter().write(objectMapper.writeValueAsString(
                                    Result.error(403, "没有访问权限")));
                        }))
                .addFilterBefore(jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
