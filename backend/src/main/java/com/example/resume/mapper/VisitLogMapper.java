package com.example.resume.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.resume.entity.VisitLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 访问日志 Mapper：基础 CRUD 由 BaseMapper 提供，
 * 统计类聚合查询在此声明。
 */
@Mapper
public interface VisitLogMapper extends BaseMapper<VisitLog> {

    /** 独立 IP 总数（去重） */
    @Select("SELECT COUNT(DISTINCT ip) FROM visit_log")
    long countDistinctIp();

    /**
     * 会话窗口去重：同一浏览器会话（session_id 由前端持久化生成）在指定时间后是否已有访问
     */
    @Select("SELECT COUNT(*) FROM visit_log WHERE session_id = #{sessionId} AND visit_time >= #{since}")
    long countRecentBySession(@Param("sessionId") String sessionId,
                              @Param("since") LocalDateTime since);

    /**
     * 会话窗口去重（无 sessionId 时按 IP 兜底）
     */
    @Select("SELECT COUNT(*) FROM visit_log WHERE ip = #{ip} AND visit_time >= #{since}")
    long countRecentByIp(@Param("ip") String ip,
                         @Param("since") LocalDateTime since);

    /** 指定时间之后的独立 IP 数 */
    @Select("SELECT COUNT(DISTINCT ip) FROM visit_log WHERE visit_time >= #{since}")
    long countDistinctIpSince(@Param("since") LocalDateTime since);

    /**
     * 按日期统计访问量与独立 IP，用于趋势图
     * 返回字段：d(日期), c(访问次数), u(独立IP数)
     */
    @Select("""
            SELECT DATE(visit_time) AS d,
                   COUNT(*) AS c,
                   COUNT(DISTINCT ip) AS u
            FROM visit_log
            WHERE visit_time >= #{since}
            GROUP BY DATE(visit_time)
            ORDER BY d
            """)
    List<Map<String, Object>> dailyTrend(@Param("since") LocalDateTime since);

    /** 按访问来源（Referer 原始值）分组统计 */
    @Select("""
            SELECT COALESCE(NULLIF(TRIM(referer), ''), '直接访问') AS src,
                   COUNT(*) AS c
            FROM visit_log
            GROUP BY src
            ORDER BY c DESC
            LIMIT 20
            """)
    List<Map<String, Object>> countByReferer();

    /** 最近 N 天访问最活跃的 IP Top N */
    @Select("""
            SELECT ip, COUNT(*) AS c
            FROM visit_log
            WHERE visit_time >= #{since}
            GROUP BY ip
            ORDER BY c DESC
            LIMIT #{limit}
            """)
    List<Map<String, Object>> topIps(@Param("since") LocalDateTime since,
                                     @Param("limit") int limit);
}
