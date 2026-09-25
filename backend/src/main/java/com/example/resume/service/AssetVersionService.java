package com.example.resume.service;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 静态资源（上传图片）版本指纹：
 * 出参 URL 自动拼上文件最后修改时间 ?v=ts —— 文件被替换后 v 立即变化，
 * 浏览器会当作新资源重新请求，访客无需手动刷新即可看到最新图片。
 * mtime 做短 TTL 缓存以减少磁盘 IO；后台"刷新缓存"会主动清空。
 */
@Slf4j
@Service
public class AssetVersionService {

    @Value("${app.upload.dir:./uploads}")
    private String uploadDir;

    @Value("${app.upload.url-prefix:/uploads}")
    private String urlPrefix;

    private Path uploadRoot;

    /** 相对路径 → 指纹时间戳（短缓存） */
    private final ConcurrentHashMap<String, long[]> cache = new ConcurrentHashMap<>();
    private static final long TTL_MS = 3000;

    @PostConstruct
    void init() {
        uploadRoot = Paths.get(uploadDir).toAbsolutePath().normalize();
        log.info("资源指纹服务就绪: root={}, prefix={}", uploadRoot, urlPrefix);
    }

    /**
     * 给 /uploads 开头的相对 URL 附加文件版本指纹；
     * 非上传资源、文件不存在时原样返回。
     */
    public String decorate(String url) {
        String clean = stripVersion(url);
        if (clean == null || clean.isBlank()) {
            return clean;
        }
        if (!clean.startsWith(urlPrefix + "/")) {
            return clean;
        }
        String rel = clean.substring(urlPrefix.length() + 1);
        Long version = versionOf(rel);
        if (version == null) {
            return clean;
        }
        return clean + "?v=" + version;
    }

    private Long versionOf(String relativePath) {
        long now = System.currentTimeMillis();
        long[] hit = cache.get(relativePath);
        if (hit != null && now - hit[1] < TTL_MS) {
            return hit[0] == 0L ? null : hit[0];
        }
        File file = uploadRoot.resolve(relativePath).toFile();
        long mtime = file.isFile() ? file.lastModified() : 0L;
        cache.put(relativePath, new long[]{mtime, now});
        return mtime == 0L ? null : mtime;
    }

    /** 后台手动刷新：清空指纹缓存（随后请求按最新文件 mtime 重新计算） */
    public long refresh() {
        int size = cache.size();
        cache.clear();
        long ts = System.currentTimeMillis();
        log.info("资源指纹缓存已清空（{} 项），全站图片版本已刷新", size);
        return ts;
    }

    /**
     * 入库前清洗：去掉 URL 上由本服务附加的 v 参数，保证数据库只存干净路径。
     * （前端可能把出参带 ?v= 的 URL 原样提交回来）
     */
    public static String stripVersion(String url) {
        if (url == null) {
            return null;
        }
        int q = url.indexOf('?');
        if (q < 0) {
            return url;
        }
        String base = url.substring(0, q);
        String query = url.substring(q + 1);
        StringBuilder kept = new StringBuilder();
        for (String pair : query.split("&")) {
            if (pair.isEmpty() || pair.startsWith("v=")) {
                continue;
            }
            if (kept.length() > 0) {
                kept.append('&');
            }
            kept.append(pair);
        }
        return kept.length() == 0 ? base : base + "?" + kept;
    }
}
