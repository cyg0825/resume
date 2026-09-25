package com.example.resume.entity;

import com.example.resume.common.AssetUrl;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 作品集（按简历主人姓名归属，与简历版本无关：
 * 同一人名下的所有版本共享同一份；改名视为另一个人，作品不迁移；删除版本不删作品）
 */
@Data
@TableName("portfolio")
public class Portfolio {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 归属人姓名。保持默认更新策略：编辑接口刻意传 null 表示“不允许修改归属人”，
     * 此时该字段会被 MyBatis-Plus 忽略。
     */
    private String ownerName;

    private String title;

    /** 封面图 URL（可清空） */
    @AssetUrl
    @TableField(updateStrategy = FieldStrategy.IGNORED)
    private String cover;

    /** 项目链接（可清空） */
    @TableField(updateStrategy = FieldStrategy.IGNORED)
    private String url;

    /** 项目描述（可清空） */
    @TableField(updateStrategy = FieldStrategy.IGNORED)
    private String description;

    private Integer sort;
}
