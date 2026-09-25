package com.example.resume.common;

import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 基于内存的固定窗口限流器：同一 IP 60 秒内最多访问指定次数。
 * 用于专属链接访问接口，防止暴力枚举 token。
 */
@Component
public class SimpleRateLimiter {

    private static final int DEFAULT_LIMIT = 20;
    private static final long WINDOW_MS = 60_000L;

    private final Map<String, Deque<Long>> records = new ConcurrentHashMap<>();

    public boolean allow(String key) {
        return allow(key, DEFAULT_LIMIT);
    }

    public boolean allow(String key, int limit) {
        long now = System.currentTimeMillis();
        Deque<Long> deque = records.computeIfAbsent(key, k -> new ArrayDeque<>());
        synchronized (deque) {
            while (!deque.isEmpty() && now - deque.peekFirst() > WINDOW_MS) {
                deque.pollFirst();
            }
            if (deque.size() >= limit) {
                return false;
            }
            deque.addLast(now);
            return true;
        }
    }
}
