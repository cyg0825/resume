package com.example.resume.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 访客凭链接 token 访问成功后返回：
 * 前端保存 visitorToken，后续所有简历接口携带它访问
 */
@Data
@Builder
public class ShareAccessVO {

    /** 访客访问令牌（默认 60 天，取 app.jwt.share-expire；与后台管理员 JWT 区分） */
    private String visitorToken;

    private Long linkId;

    /** 该链接绑定的简历版本（null=默认版本） */
    private Long versionId;

    private String remark;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime expireTime;

    private Integer maxViews;

    private Integer viewCount;
}
