package com.example.resume.entity;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 技能特长
 */
@Data
@TableName("skill")
public class Skill {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long versionId;

    /** 技能分类：前端/后端/数据库/运维等（可清空） */
    @TableField(updateStrategy = FieldStrategy.IGNORED)
    private String category;

    private String name;

    /** 熟练度 0-100 */
    private Integer level;

    private Integer sort;
}
