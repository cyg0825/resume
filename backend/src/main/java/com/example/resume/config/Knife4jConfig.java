package com.example.resume.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Knife4j / springdoc-openapi 接口文档配置。
 * 文档地址：http://localhost:8081/doc.html
 * 同时按“前台公开接口 / 后台管理接口(JWT)”分为两个分组。
 */
@Configuration
public class Knife4jConfig {

    /** 文档页全局鉴权方案名称：在 Knife4j 的“文档管理 → 全局参数设置”中录入 JWT */
    private static final String SECURITY_SCHEME_NAME = "Bearer-JWT";

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("个人简历网站 API 文档")
                        .description("""
                                基于 Vue 3 + Spring Boot 3 的个人简历网站后端接口。
                                - 前台公开接口：无需登录，可直接调试
                                - 后台管理接口：需先调用 /api/auth/login 获取 JWT，\
                                在文档右上角“Authorize”中填入 token（无需 Bearer 前缀，系统会自动拼接）
                                - 统一响应格式：{ "code": 200, "msg": "success", "data": ... }""")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Resume Website")
                                .email("admin@example.com")))
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME,
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .in(SecurityScheme.In.HEADER)
                                        .name("Authorization")
                                        .description("JWT 鉴权，格式：Bearer {token}")))
                // 全局应用鉴权方案（未填写 token 时公开接口仍可正常匿名调用）
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME));
    }

    /** 前台公开接口分组 */
    @Bean
    public GroupedOpenApi publicGroup() {
        return GroupedOpenApi.builder()
                .group("01-前台公开接口")
                .pathsToMatch(
                        "/api/auth/**",
                        "/api/profile",
                        "/api/educations",
                        "/api/experiences",
                        "/api/skills",
                        "/api/honors",
                        "/api/portfolios",
                        "/api/config",
                        "/api/visit",
                        "/api/ai/chat"
                )
                .build();
    }

    /** 后台管理接口分组（全部需要 JWT） */
    @Bean
    public GroupedOpenApi adminGroup() {
        return GroupedOpenApi.builder()
                .group("02-后台管理接口(JWT)")
                .pathsToMatch("/api/admin/**")
                .build();
    }
}
