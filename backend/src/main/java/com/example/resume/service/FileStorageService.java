package com.example.resume.service;

import com.example.resume.common.BusinessException;
import com.example.resume.util.ImageCompressUtil;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Set;
import java.util.UUID;

/**
 * 本地文件存储（图片上传），按日期分目录，返回可访问 URL
 */
@Slf4j
@Service
public class FileStorageService {

    private static final Set<String> ALLOWED_EXTENSIONS =
            Set.of(".jpg", ".jpeg", ".png", ".gif", ".webp", ".svg", ".bmp");

    @Value("${app.upload.dir}")
    private String uploadDir;

    @Value("${app.upload.url-prefix}")
    private String urlPrefix;

    @PostConstruct
    public void init() {
        // 启动时打印实际生效的上传目录与可写性，部署排查用
        String absolute = Paths.get(uploadDir).toAbsolutePath().normalize().toString();
        boolean writable;
        try {
            Files.createDirectories(Paths.get(absolute));
            writable = Files.isWritable(Paths.get(absolute));
        } catch (Exception e) {
            writable = false;
        }
        log.info("========== 文件上传目录配置生效: uploadDir={} | 绝对路径={} | 可写={} ==========",
                uploadDir, absolute, writable);
    }

    public String storeImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(400, "上传文件不能为空");
        }
        String original = StringUtils.cleanPath(
                file.getOriginalFilename() == null ? "" : file.getOriginalFilename());
        String extension = "";
        int dot = original.lastIndexOf('.');
        if (dot >= 0) {
            extension = original.substring(dot).toLowerCase();
        }
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new BusinessException(400, "仅支持图片格式：jpg/jpeg/png/gif/webp/svg/bmp");
        }

        String datePart = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM"));
        String baseName = UUID.randomUUID().toString().replace("-", "");

        // 大图自动压缩到 150KB 以内（JPEG，见 ImageCompressUtil.MAX_BYTES）；压缩后的文件统一用 .jpg
        byte[] compressed = null;
        try {
            compressed = ImageCompressUtil.compressIfNeeded(file.getBytes(), extension);
        } catch (IOException e) {
            log.warn("读取上传文件失败，改用原样保存: {}", e.getMessage());
        }
        String savedExt = compressed != null ? ".jpg" : extension;
        String fileName = baseName + savedExt;
        Path targetDir = Paths.get(uploadDir, datePart);
        Path targetFile = targetDir.resolve(fileName);
        try {
            Files.createDirectories(targetDir);
            // 子目录可能已存在但属主不是运行用户（如用 root 解压过上传目录），
            // 提前做可写性检查，给出可直接执行的修复指引，避免裸 Permission denied
            if (!Files.isWritable(targetDir)) {
                log.error("上传目录不可写: {}（运行用户={}）", targetDir.toAbsolutePath(),
                        System.getProperty("user.name"));
                throw new BusinessException(500, "上传目录不可写: " + targetDir.toAbsolutePath()
                        + "（当前运行用户: " + System.getProperty("user.name") + "）。"
                        + "请在服务器执行: chown -R " + System.getProperty("user.name") + ":"
                        + System.getProperty("user.name") + " " + Paths.get(uploadDir).toAbsolutePath()
                        + " && chmod -R 755 " + Paths.get(uploadDir).toAbsolutePath());
            }
            if (compressed != null) {
                Files.write(targetFile, compressed);
                log.info("图片已压缩保存: {} (原始 {}KB → {}KB)",
                        fileName, file.getSize() / 1024, compressed.length / 1024);
            } else {
                file.transferTo(targetFile.toAbsolutePath().toFile());
            }
        } catch (BusinessException e) {
            throw e;
        } catch (IOException e) {
            log.error("图片保存失败，目标路径: {}", targetFile.toAbsolutePath(), e);
            boolean permissionDenied = e instanceof java.nio.file.AccessDeniedException
                    || (e.getMessage() != null && e.getMessage().contains("Permission denied"));
            String hint = permissionDenied
                    ? "（权限不足，请在服务器执行: chown -R " + System.getProperty("user.name") + ":"
                        + System.getProperty("user.name") + " " + Paths.get(uploadDir).toAbsolutePath()
                        + " && chmod -R 755 " + Paths.get(uploadDir).toAbsolutePath() + "）"
                    : "";
            throw new BusinessException(500, "文件保存失败: 目标目录=" + targetDir.toAbsolutePath()
                    + " | 原因(" + e.getClass().getSimpleName() + "): " + e.getMessage() + hint);
        }
        return urlPrefix + "/" + datePart + "/" + fileName;
    }
}
