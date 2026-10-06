package com.example.resume.service;

import com.example.resume.entity.VisitLog;
import com.example.resume.mapper.VisitLogMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 访客访问统计：
 * - 记录每次访问（IP、UA、来源、路径、时间）
 * - 同一会话窗口内（默认 30 分钟）只计一次，避免刷新页面把访问次数刷高
 * - 总量、独立 IP（去重）、今日数据、近 N 天趋势、来源分布、活跃 IP
 */
@Service
@RequiredArgsConstructor
public class VisitService {

    private final VisitLogMapper visitLogMapper;

    /** 会话去重窗口（分钟）：同一浏览器会话在该窗口内多次打开/刷新只记一次 */
    @Value("${app.visit.session-window-minutes:30}")
    private long sessionWindowMinutes;

    /**
     * @return true 表示本次已记录；false 表示落在会话去重窗口内被忽略
     */
    public boolean record(String ip, String userAgent, String referer, String path, String sessionId) {
        LocalDateTime since = LocalDateTime.now().minusMinutes(sessionWindowMinutes);
        long recent = StringUtils.hasText(sessionId)
                ? visitLogMapper.countRecentBySession(sessionId, since)
                : visitLogMapper.countRecentByIp(ip, since);
        if (recent > 0) {
            return false;
        }

        VisitLog log = new VisitLog();
        log.setIp(ip);
        log.setUserAgent(truncate(userAgent, 500));
        log.setReferer(truncate(referer, 500));
        log.setPath(truncate(path, 255));
        log.setSessionId(truncate(sessionId, 64));
        visitLogMapper.insert(log);
        return true;
    }

    /** 总览数据 */
    public Map<String, Object> overview() {
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("totalVisits", visitLogMapper.selectCount(null));
        data.put("uniqueVisitors", visitLogMapper.countDistinctIp());
        data.put("todayVisits", visitLogMapper.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<VisitLog>()
                        .ge(VisitLog::getVisitTime, todayStart)));
        data.put("todayUniqueVisitors", visitLogMapper.countDistinctIpSince(todayStart));
        // 会话窗口透出给后台，指标卡文案才不会把默认值写死
        data.put("sessionWindowMinutes", sessionWindowMinutes);
        return data;
    }

    /**
     * 近 N 天每日趋势，补齐没有访问的日期（便于前端直接画图）
     */
    public List<Map<String, Object>> trend(int days) {
        LocalDate today = LocalDate.now();
        LocalDateTime since = today.minusDays(days - 1L).atStartOfDay();

        List<Map<String, Object>> rows = visitLogMapper.dailyTrend(since);
        Map<LocalDate, Map<String, Object>> rowMap = new LinkedHashMap<>();
        for (Map<String, Object> row : rows) {
            LocalDate date = toLocalDate(row.get("d"));
            rowMap.put(date, row);
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (int i = 0; i < days; i++) {
            LocalDate date = today.minusDays(days - 1L - i);
            Map<String, Object> row = rowMap.get(date);
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("date", date.toString());
            item.put("visits", row != null ? ((Number) row.get("c")).longValue() : 0L);
            item.put("uniqueVisitors", row != null ? ((Number) row.get("u")).longValue() : 0L);
            result.add(item);
        }
        return result;
    }

    /**
     * 访问来源分布：把 Referer 归一化为站点域名，空值归为“直接访问”
     */
    public List<Map<String, Object>> sources() {
        List<Map<String, Object>> rows = visitLogMapper.countByReferer();
        Map<String, Long> hostCount = new LinkedHashMap<>();
        for (Map<String, Object> row : rows) {
            String src = String.valueOf(row.get("src"));
            long count = ((Number) row.get("c")).longValue();
            String host = "直接访问".equals(src) ? "直接访问" : extractHost(src);
            hostCount.merge(host, count, Long::sum);
        }
        return hostCount.entrySet().stream()
                .map(e -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("source", e.getKey());
                    m.put("count", e.getValue());
                    return m;
                })
                .sorted((a, b) -> Long.compare((Long) b.get("count"), (Long) a.get("count")))
                .collect(Collectors.toList());
    }

    /** 近 N 天活跃 IP Top 10 */
    public List<Map<String, Object>> topIps(int days) {
        LocalDateTime since = LocalDate.now().minusDays(days - 1L).atStartOfDay();
        List<Map<String, Object>> rows = visitLogMapper.topIps(since, 10);
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("ip", String.valueOf(row.get("ip")));
            item.put("count", ((Number) row.get("c")).longValue());
            result.add(item);
        }
        return result;
    }

    private String extractHost(String url) {
        try {
            URI uri = URI.create(url);
            String host = uri.getHost();
            return StringUtils.hasText(host) ? host : url;
        } catch (Exception e) {
            return url;
        }
    }

    private LocalDate toLocalDate(Object value) {
        if (value instanceof LocalDate d) {
            return d;
        }
        if (value instanceof java.sql.Date d) {
            return d.toLocalDate();
        }
        if (value instanceof java.util.Date d) {
            return d.toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate();
        }
        return LocalDate.parse(String.valueOf(value));
    }

    private String truncate(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        return value.length() > maxLength ? value.substring(0, maxLength) : value;
    }
}
