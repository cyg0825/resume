package com.example.resume.util;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.util.StringUtils;

/**
 * IP 工具类：优先取代理链中的真实客户端 IP
 */
public final class IpUtils {

    private IpUtils() {
    }

    public static String getClientIp(HttpServletRequest request) {
        String[] headers = {
                "X-Forwarded-For", "X-Real-IP", "Proxy-Client-IP",
                "WL-Proxy-Client-IP", "HTTP_CLIENT_IP", "HTTP_X_FORWARDED_FOR"
        };
        for (String header : headers) {
            String ip = request.getHeader(header);
            if (StringUtils.hasText(ip) && !"unknown".equalsIgnoreCase(ip)) {
                // X-Forwarded-For 可能是逗号分隔的多级代理，第一个为真实 IP
                int comma = ip.indexOf(',');
                String candidate = (comma >= 0 ? ip.substring(0, comma) : ip).trim();
                // 头值以逗号开头或整段为空时视为无效，继续试下一个头
                if (StringUtils.hasText(candidate) && !"unknown".equalsIgnoreCase(candidate)) {
                    return candidate;
                }
            }
        }
        return request.getRemoteAddr();
    }
}
