package com.example.resume.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.resume.entity.ShareLink;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface ShareLinkMapper extends BaseMapper<ShareLink> {

    /**
     * 计数 +1（带有效性条件，并发安全）：
     * 仅当链接启用、未过期、次数未满时才更新，返回受影响行数。
     */
    @Update("UPDATE share_link SET view_count = view_count + 1, "
            + "last_view_time = NOW(), last_view_ip = #{ip} "
            + "WHERE id = #{id} AND enabled = 1 "
            + "AND (expire_time IS NULL OR expire_time > NOW()) "
            + "AND (max_views IS NULL OR view_count < max_views)")
    int incrViewIfValid(@Param("id") Long id, @Param("ip") String ip);
}
