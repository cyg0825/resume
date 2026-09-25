package com.example.resume.entity;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;

/**
 * 教育经历
 */
@Data
@TableName("education")
public class Education {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long versionId;

    /** 可空业务字段使用 IGNORED 策略：编辑时清空（传 null）也要真正写入 null */
    @TableField(updateStrategy = FieldStrategy.IGNORED)
    private String school;

    @TableField(updateStrategy = FieldStrategy.IGNORED)
    private String major;

    @TableField(updateStrategy = FieldStrategy.IGNORED)
    private String degree;

    @TableField(updateStrategy = FieldStrategy.IGNORED)
    private LocalDate startDate;

    @TableField(updateStrategy = FieldStrategy.IGNORED)
    private LocalDate endDate;

    @TableField(updateStrategy = FieldStrategy.IGNORED)
    private String description;

    private Integer sort;
}
