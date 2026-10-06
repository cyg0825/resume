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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * 启动时基础数据兜底初始化：
 * - 默认管理员账号（user 表为空时创建，用户名/密码来自 app.admin.* 配置项）
 * - 默认简历版本（id=1）及其空白个人信息
 * - 默认站点配置（id=1）
 * 注意：示例简历内容由 SQL 脚本导入，这里只保证系统可登录、可运行。
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

    @Value("${app.admin.username}")
    private String adminUsername;

    @Value("${app.admin.password}")
    private String adminPassword;

    @Override
    public void run(String... args) {
        initAdmin();
        initDefaultVersion();
        initSiteConfig();
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