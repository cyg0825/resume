package com.example.resume.init;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.resume.entity.Profile;
import com.example.resume.entity.ResumeVersion;
import com.example.resume.entity.SiteConfig;
import com.example.resume.entity.User;
import com.example.resume.mapper.ProfileMapper;
import com.example.resume.mapper.ResumeVersionMapper;
import com.example.resume.mapper.SiteConfigMapper;
import com.example.resume.mapper.UserMapper;
import com.example.resume.service.SiteConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.jdbc.datasource.init.ScriptException;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * 启动时基础数据兜底初始化：
 * - 示例简历内容（data.sql）：仅当 resume_version 为空时导入一次
 * - 默认管理员账号（user 表为空时创建，用户名/密码来自 app.admin.* 配置项）
 * - 默认简历版本（id=1）及其空白个人信息
 * - 默认站点配置（id=1）
 * 每一步都先看表里有没有数据，因此反复重启不会覆盖运行期改过的内容。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserMapper userMapper;
    private final ResumeVersionMapper versionMapper;
    private final ProfileMapper profileMapper;
    private final SiteConfigMapper siteConfigMapper;
    private final PasswordEncoder passwordEncoder;
    private final DataSource dataSource;

    @Value("${app.admin.username}")
    private String adminUsername;

    @Value("${app.admin.password}")
    private String adminPassword;

    @Override
    public void run(String... args) {
        seedDemoDataIfEmpty();
        initAdmin();
        initDefaultVersion();
        initSiteConfig();
    }

    /**
     * 示例简历内容只在空库灌一次：有版本记录说明这套库在用，重启不再动它。
     * 范围边界一：data.sql 不入版本库，也不放在 resources 下（打进去就等于把真实简历带进镜像），
     *              因此正常构建出的 jar 里没有这份脚本，导入直接跳过，由后面的兜底逻辑建空白版本。
     * 范围边界二：如果手工把 resume_version 全删光，下次重启会把这份内容再灌一遍。
     */
    private void seedDemoDataIfEmpty() {
        Long versionCount = versionMapper.selectCount(null);
        if (versionCount != null && versionCount > 0) {
            return;
        }
        ClassPathResource script = new ClassPathResource("data.sql");
        if (!script.exists()) {
            log.info("未找到 data.sql（该文件不入库），跳过简历内容导入，按空白版本兜底");
            return;
        }
        try (Connection connection = dataSource.getConnection()) {
            ScriptUtils.executeSqlScript(connection,
                    new EncodedResource(script, StandardCharsets.UTF_8));
            log.info("简历内容脚本已导入（触发条件：resume_version 为空）");
        } catch (SQLException | ScriptException e) {
            log.error("简历内容脚本导入失败，改由兜底逻辑创建空白版本: {}", e.getMessage());
        }
    }

    private void initAdmin() {
        Long count = userMapper.selectCount(null);
        if (count != null && count > 0) {
            return;
        }
        User admin = new User();
        admin.setUsername(adminUsername);
        admin.setPassword(passwordEncoder.encode(adminPassword));
        admin.setNickname("管理员");
        userMapper.insert(admin);
        log.info("初始管理员账号已创建: {}", adminUsername);
    }

    private void initDefaultVersion() {
        Long versionCount = versionMapper.selectCount(null);
        boolean hasAnyVersion = versionCount != null && versionCount > 0;
        ResumeVersion v1 = versionMapper.selectById(1L);

        // 仅在全新空库时创建兜底版本；
        // 用户已删除 id=1 并使用自己版本的情况下，绝不重建空壳污染版本列表
        if (v1 == null && !hasAnyVersion) {
            v1 = new ResumeVersion();
            v1.setId(1L);
            v1.setVersionName("默认版本");
            v1.setDescription("系统初始化的默认简历版本");
            v1.setIsDefault(1);
            versionMapper.insert(v1);
        }

        // 异常数据兜底：id=1 存在但缺少个人信息时补齐
        if (versionMapper.selectById(1L) != null) {
            Long profileCount = profileMapper.selectCount(new LambdaQueryWrapper<Profile>()
                    .eq(Profile::getVersionId, 1L));
            if (profileCount == null || profileCount == 0) {
                Profile profile = new Profile();
                profile.setVersionId(1L);
                profile.setName("张三");
                profile.setJobTitle("全栈工程师");
                profile.setSlogan("持续学习，热爱技术");
                profileMapper.insert(profile);
            }
        }
    }

    private void initSiteConfig() {
        if (siteConfigMapper.selectById(1L) == null) {
            siteConfigMapper.insert(SiteConfigService.defaultConfig());
        }
    }
}