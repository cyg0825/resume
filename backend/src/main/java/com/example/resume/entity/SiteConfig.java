package com.example.resume.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 站点配置（单行表，id 固定为 1）：
 * 维护主题默认值、站点标题、AI 问答开关等
 */
@Data
@TableName("site_config")
public class SiteConfig {

    @TableId(type = IdType.INPUT)
    private Long id;

    /** 站点标题（可清空） */
    @TableField(updateStrategy = FieldStrategy.IGNORED)
    private String siteTitle;

    /** 默认主题：default / dark / fresh */
    @TableField(updateStrategy = FieldStrategy.IGNORED)
    private String defaultTheme;

    /** AI 问答开关：1 开启，0 关闭 */
    private Integer aiEnabled;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
