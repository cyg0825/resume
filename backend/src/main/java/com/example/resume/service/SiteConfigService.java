package com.example.resume.service;

import com.example.resume.entity.SiteConfig;
import com.example.resume.mapper.SiteConfigMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 站点配置服务（单行配置，id 固定为 1）
 */
@Service
@RequiredArgsConstructor
public class SiteConfigService {

    private static final Long CONFIG_ID = 1L;

    private final SiteConfigMapper siteConfigMapper;

    /**
     * 获取配置，不存在则返回内存默认值（不抛异常，保证前台可用）
     */
    public SiteConfig getConfig() {
        SiteConfig config = siteConfigMapper.selectById(CONFIG_ID);
        if (config == null) {
            config = defaultConfig();
        }
        return config;
    }

    public void update(SiteConfig param) {
        SiteConfig exists = siteConfigMapper.selectById(CONFIG_ID);
        param.setId(CONFIG_ID);
        if (exists == null) {
            siteConfigMapper.insert(param);
        } else {
            siteConfigMapper.updateById(param);
        }
    }

    /** 数据库缺失时的兜底默认配置，同时供初始化器使用 */
    public static SiteConfig defaultConfig() {
        SiteConfig config = new SiteConfig();
        config.setId(CONFIG_ID);
        config.setSiteTitle("个人简历");
        config.setDefaultTheme("default");
        config.setAiEnabled(1);
        return config;
    }
}
