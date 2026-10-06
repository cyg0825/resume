package com.example.resume.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * update_time 自动填充。DDL 里只有 DEFAULT CURRENT_TIMESTAMP，
 * 没有 MetaObjectHandler 时该列插入后永不变化。
 * 只对标注了 @TableField(fill = ...) 的实体字段生效。
 */
@Component
public class MyBatisFillConfig implements MetaObjectHandler {

    @Override
    public void insertFill(MetaObject metaObject) {
        strictInsertFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        strictUpdateFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
    }
}
