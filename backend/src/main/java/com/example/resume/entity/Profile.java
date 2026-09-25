package com.example.resume.entity;

import com.example.resume.common.AssetUrl;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 个人信息（每个简历版本一条）
 */
@Data
@TableName("profile")
public class Profile {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属简历版本 */
    private Long versionId;

    /** 可空业务字段使用 IGNORED 策略：编辑时清空（传 null）也要真正写入 null */
    @TableField(updateStrategy = FieldStrategy.IGNORED)
    private String name;

    @TableField(updateStrategy = FieldStrategy.IGNORED)
    private String jobTitle;

    @TableField(updateStrategy = FieldStrategy.IGNORED)
    private String slogan;

    @AssetUrl
    @TableField(updateStrategy = FieldStrategy.IGNORED)
    private String avatar;

    @TableField(updateStrategy = FieldStrategy.IGNORED)
    private String email;

    @TableField(updateStrategy = FieldStrategy.IGNORED)
    private String phone;

    @TableField(updateStrategy = FieldStrategy.IGNORED)
    private String wechat;

    @TableField(updateStrategy = FieldStrategy.IGNORED)
    private String github;

    /** Gitee 主页地址 */
    @TableField(updateStrategy = FieldStrategy.IGNORED)
    private String gitee;

    /** CSDN 博客地址 */
    @TableField(updateStrategy = FieldStrategy.IGNORED)
    private String csdn;

    @TableField(updateStrategy = FieldStrategy.IGNORED)
    private String address;

    @TableField(updateStrategy = FieldStrategy.IGNORED)
    private String about;

    private LocalDateTime updateTime;
}
