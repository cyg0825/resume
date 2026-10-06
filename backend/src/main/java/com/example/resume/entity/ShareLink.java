package com.example.resume.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 专属分享链接：只有拿到有效链接的访客才能查看简历。
 * - token：64 位随机十六进制串，不可枚举
 * - expireTime 为空表示永久有效；maxViews 为空表示不限次数
 * - versionId 可绑定指定简历版本，为空时访客看到默认版本
 * - enabled=0 表示后台已吊销
 */
@Data
@TableName("share_link")
public class ShareLink {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 随机访问串（64 位 hex） */
    private String token;

    /** 备注：发给谁，如"某公司 HR" */
    private String remark;

    /** 绑定的简历版本 ID，NULL=默认版本 */
    private Long versionId;

    /** 过期时间，NULL=永久 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime expireTime;

    /** 最大访问次数，NULL=不限 */
    private Integer maxViews;

    /** 已访问次数（每次通过链接进入 +1） */
    private Integer viewCount;

    /** 1=启用 0=已吊销 */
    private Integer enabled;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastViewTime;

    private String lastViewIp;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
