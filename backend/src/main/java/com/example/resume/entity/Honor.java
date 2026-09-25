package com.example.resume.entity;

import com.example.resume.common.AssetUrl;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;

/**
 * 荣誉证书（按简历主人姓名归档，与简历版本无关：
 * 同一人名下的所有版本共享同一份；改名视为另一个人，荣誉不迁移；删除版本不删除荣誉）
 */
@Data
@TableName("honor")
public class Honor {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 归属人姓名。保持默认更新策略：编辑接口刻意传 null 表示“不允许修改归属人”，
     * 此时该字段会被 MyBatis-Plus 忽略。
     */
    private String ownerName;

    /** 荣誉/证书名称（含奖项等次，如：蓝桥杯省赛三等奖） */
    @TableField(updateStrategy = FieldStrategy.IGNORED)
    private String title;

    /** 证书图片 URL（可清空，前台点击卡片进入详情查看） */
    @AssetUrl
    @TableField(updateStrategy = FieldStrategy.IGNORED)
    private String image;

    /** 颁发机构 */
    @TableField(updateStrategy = FieldStrategy.IGNORED)
    private String issuer;

    /** 级别标签：国家级 / 省级 / 市级 / 校级 / 高级 / 中级 / 初级 / 荣誉证书 等 */
    @TableField(updateStrategy = FieldStrategy.IGNORED)
    private String level;

    /** 获奖日期（可清空） */
    @TableField(updateStrategy = FieldStrategy.IGNORED)
    private LocalDate honorDate;

    /** 补充说明 */
    @TableField(updateStrategy = FieldStrategy.IGNORED)
    private String description;

    private Integer sort;
}
