package com.example.resume.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 后台生成专属链接请求
 */
@Data
public class ShareLinkCreateRequest {

    @NotBlank(message = "请填写备注（发给谁）")
    @Size(max = 100, message = "备注最多 100 字")
    private String remark;

    /** 绑定的简历版本 ID，不传=默认版本 */
    private Long versionId;

    /** 过期时间，不传=永久有效 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime expireTime;

    /** 最大访问次数，不传/空=不限 */
    private Integer maxViews;
}
