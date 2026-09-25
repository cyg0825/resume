package com.example.resume.common;

import com.example.resume.config.AssetUrlSerializer;
import com.fasterxml.jackson.annotation.JacksonAnnotationsInside;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标在实体的图片 URL 字段上（头像/封面/证书图），
 * 序列化返回给前端时自动附加文件版本指纹，保证图片更新后访客立即看到。
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@JacksonAnnotationsInside
@JsonSerialize(using = AssetUrlSerializer.class)
public @interface AssetUrl {
}
