package com.example.resume.util;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.MemoryCacheImageOutputStream;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.Set;

/**
 * 图片压缩工具：上传时把大图缩放到限定边长内，并以 JPEG 迭代降质，
 * 直到体积不超过目标大小（150KB）；若调质量仍超标则逐级缩小长边，保证前台加载速度。
 * SVG/GIF 等不适合位图压缩的格式不处理。
 */
public final class ImageCompressUtil {

    /** 压缩目标：150KB */
    public static final long MAX_BYTES = 150 * 1024L;
    /** 最长边上限（优先在该尺寸内调质量，超标再逐级缩小） */
    public static final int MAX_DIMENSION = 1600;
    /** 尺寸阶梯：质量调到最低仍超标时，逐级缩小长边 */
    private static final int[] DIMENSION_LADDER = {1600, 1366, 1152, 960};
    /** 每一级尺寸下尝试的 JPEG 质量（从高到低） */
    private static final float[] QUALITIES = {0.82f, 0.72f, 0.62f, 0.52f, 0.44f};

    private static final Set<String> COMPRESSIBLE = Set.of(".jpg", ".jpeg", ".png", ".bmp");

    private ImageCompressUtil() {
    }

    public static boolean supports(String extension) {
        return extension != null && COMPRESSIBLE.contains(extension.toLowerCase());
    }

    /**
     * 压缩图片；若原图本身不大或无需处理则返回 null（调用方原样保存）。
     *
     * @param raw       原始字节
     * @param extension 原始扩展名（小写，含点）
     * @return 压缩后的 JPEG 字节；无法解码或不支持时返回 null
     */
    public static byte[] compressIfNeeded(byte[] raw, String extension) {
        if (!supports(extension)) {
            return null;
        }
        BufferedImage src;
        try {
            src = ImageIO.read(new ByteArrayInputStream(raw));
        } catch (IOException e) {
            return null;
        }
        if (src == null) {
            return null;
        }
        // 原图已小于上限且尺寸不大：直接返回 null，保持原样（避免无谓重编码损失）
        if (raw.length <= MAX_BYTES
                && src.getWidth() <= MAX_DIMENSION && src.getHeight() <= MAX_DIMENSION) {
            return null;
        }

        // 先在尽量大的尺寸下调质量；仍超标则逐级缩小长边，保证体积达标
        byte[] best = null;
        for (int maxDim : DIMENSION_LADDER) {
            if (maxDim > Math.max(src.getWidth(), src.getHeight())) {
                continue;
            }
            BufferedImage scaled = scaleTo(src, maxDim);
            BufferedImage opaque = flattenWhite(scaled);
            if (opaque != scaled) {
                scaled.flush();
            }
            for (float q : QUALITIES) {
                byte[] out = writeJpeg(opaque, q);
                if (out == null) {
                    continue;
                }
                if (best == null || out.length < best.length) {
                    best = out;
                }
                if (out.length <= MAX_BYTES) {
                    opaque.flush();
                    src.flush();
                    return out;
                }
            }
            opaque.flush();
        }
        src.flush();
        // 最小尺寸+最低质量仍超标时，返回已得到的最小结果
        return best;
    }

    private static BufferedImage scaleTo(BufferedImage src, int maxDim) {
        int w = src.getWidth();
        int h = src.getHeight();
        int longest = Math.max(w, h);
        if (longest <= maxDim) {
            return src;
        }
        double ratio = (double) maxDim / longest;
        int nw = Math.max(1, (int) Math.round(w * ratio));
        int nh = Math.max(1, (int) Math.round(h * ratio));
        BufferedImage dst = new BufferedImage(nw, nh, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = dst.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setRenderingHint(RenderingHints.KEY_RENDERING,
                RenderingHints.VALUE_RENDER_QUALITY);
        g.drawImage(src, 0, 0, nw, nh, null);
        g.dispose();
        return dst;
    }

    /** 统一画到白底 RGB 图上（输出 JPEG 不支持透明，避免透明区变黑） */
    private static BufferedImage flattenWhite(BufferedImage img) {
        if (!img.getColorModel().hasAlpha()
                && img.getType() == BufferedImage.TYPE_INT_RGB) {
            return img;
        }
        BufferedImage rgb = new BufferedImage(img.getWidth(), img.getHeight(),
                BufferedImage.TYPE_INT_RGB);
        Graphics2D g = rgb.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, img.getWidth(), img.getHeight());
        g.drawImage(img, 0, 0, null);
        g.dispose();
        img.flush();
        return rgb;
    }

    private static byte[] writeJpeg(BufferedImage img, float quality) {
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpg");
        if (!writers.hasNext()) {
            return null;
        }
        ImageWriter writer = writers.next();
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
             MemoryCacheImageOutputStream out = new MemoryCacheImageOutputStream(bos)) {
            ImageWriteParam param = writer.getDefaultWriteParam();
            param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            param.setCompressionQuality(quality);
            writer.setOutput(out);
            writer.write(null, new IIOImage(img, null, null), param);
            out.flush();
            return bos.toByteArray();
        } catch (IOException e) {
            return null;
        } finally {
            writer.dispose();
        }
    }
}
