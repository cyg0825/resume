package com.example.resume.entity;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;

/**
 * 工作/项目经历：type=1 工作经历，type=2 项目经历
 */
@Data
@TableName("experience")
public class Experience {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long versionId;

    private Integer type;

    /** 可空业务字段使用 IGNORED 策略：编辑时清空（传 null）也要真正写入 null */
    @TableField(updateStrategy = FieldStrategy.IGNORED)
    private String company;

    @TableField(updateStrategy = FieldStrategy.IGNORED)
    private String position;

    @TableField(updateStrategy = FieldStrategy.IGNORED)
    private LocalDate startDate;

    @TableField(updateStrategy = FieldStrategy.IGNORED)
    private LocalDate endDate;

    @TableField(updateStrategy = FieldStrategy.IGNORED)
    private String description;

    @TableField(updateStrategy = FieldStrategy.IGNORED)
    private String techStack;

    private Integer sort;
}
