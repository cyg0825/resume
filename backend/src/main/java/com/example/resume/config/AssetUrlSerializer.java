package com.example.resume.config;

import com.example.resume.service.AssetVersionService;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;

/**
 * {@link com.example.resume.common.AssetUrl} 字段序列化器：
 * 输出前通过 {@link AssetVersionService} 给图片 URL 附加版本指纹。
 */
public class AssetUrlSerializer extends JsonSerializer<String> {

    @Override
    public void serialize(String value, JsonGenerator gen, SerializerProvider serializers)
            throws IOException {
        if (value == null) {
            gen.writeNull();
            return;
        }
        AssetVersionService service = SpringContextHolder.getBean(AssetVersionService.class);
        gen.writeString(service != null ? service.decorate(value) : value);
    }
}
