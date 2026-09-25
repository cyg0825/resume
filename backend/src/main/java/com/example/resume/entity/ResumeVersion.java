package com.example.resume.entity;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 简历版本：每套版本拥有独立的个人信息、教育、经历、技能、作品数据
 */
@Data
@TableName("resume_version")
public class ResumeVersion {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 版本名称，如：Java后端版、前端开发版 */
    private String versionName;

    /** 版本说明（可清空） */
    @TableField(updateStrategy = FieldStrategy.IGNORED)
    private String description;

    /** 是否为当前默认展示版本：0 否 1 是 */
    private Integer isDefault;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
