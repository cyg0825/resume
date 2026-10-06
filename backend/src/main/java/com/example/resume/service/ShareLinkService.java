package com.example.resume.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.resume.common.BusinessException;
import com.example.resume.dto.ShareLinkCreateRequest;
import com.example.resume.entity.ShareLink;
import com.example.resume.mapper.ResumeVersionMapper;
import com.example.resume.mapper.ShareLinkMapper;
import com.example.resume.security.JwtUtil;
import com.example.resume.vo.ShareAccessVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;

/**
 * 专属分享链接：生成、校验访问、吊销。
 * 访客凭 token 换取访客 JWT，之后凭 JWT 只读访问简历接口。
 * 链接自身的 expire_time 每次请求都会回查，早于 JWT 到期时以链接为准。
 */
@Service
@RequiredArgsConstructor
public class ShareLinkService {

    private final ShareLinkMapper shareLinkMapper;
    private final ResumeVersionMapper resumeVersionMapper;
    private final JwtUtil jwtUtil;

    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * 失效原因码：前端据此选择拦截页文案，不依赖中文提示的措辞。
     * 沿用 403 语义 + 子码，HTTP 状态仍由统一响应体的 code 表达。
     */
    public static final int CODE_LINK_INVALID = 4030;
    public static final int CODE_LINK_EXPIRED = 4031;
    public static final int CODE_LINK_VIEWS_FULL = 4032;
    public static final int CODE_LINK_RACE_LOST = 4033;

    /**
     * 校验链接并计数，返回访客访问令牌
     */
    public ShareAccessVO accessByToken(String token, String ip) {
        if (token == null || token.isBlank()) {
            throw new BusinessException(400, "链接无效");
        }

        ShareLink link = shareLinkMapper.selectOne(
                new LambdaQueryWrapper<ShareLink>().eq(ShareLink::getToken, token.trim()));

        // 先给出精确的失效原因
        if (link == null || !Integer.valueOf(1).equals(link.getEnabled())) {
            throw new BusinessException(CODE_LINK_INVALID, "链接无效或已被吊销");
        }
        if (link.getExpireTime() != null && link.getExpireTime().isBefore(LocalDateTime.now())) {
            throw new BusinessException(CODE_LINK_EXPIRED, "链接已过期");
        }
        if (link.getMaxViews() != null
                && link.getViewCount() != null
                && link.getViewCount() >= link.getMaxViews()) {
            throw new BusinessException(CODE_LINK_VIEWS_FULL, "链接访问次数已达上限");
        }

        // 并发安全计数：条件不满足（并发耗尽/刚好过期/被吊销）时更新 0 行
        int rows = shareLinkMapper.incrViewIfValid(link.getId(), ip);
        if (rows == 0) {
            throw new BusinessException(CODE_LINK_RACE_LOST, "链接已失效（过期、次数用尽或被吊销）");
        }

        String visitorToken = jwtUtil.generateShareToken(link.getId(), link.getVersionId());
        return ShareAccessVO.builder()
                .visitorToken(visitorToken)
                .linkId(link.getId())
                .versionId(link.getVersionId())
                .remark(link.getRemark())
                .expireTime(link.getExpireTime())
                .maxViews(link.getMaxViews())
                .viewCount((link.getViewCount() == null ? 0 : link.getViewCount()) + 1)
                .build();
    }

    /**
     * 生成专属链接
     */
    public ShareLink create(ShareLinkCreateRequest req) {
        if (req.getMaxViews() != null && req.getMaxViews() <= 0) {
            throw new BusinessException(400, "最大访问次数必须大于 0");
        }
        if (req.getExpireTime() != null && req.getExpireTime().isBefore(LocalDateTime.now())) {
            throw new BusinessException(400, "过期时间不能早于当前时间");
        }
        if (req.getVersionId() != null && resumeVersionMapper.selectById(req.getVersionId()) == null) {
            throw new BusinessException(400, "绑定的简历版本不存在");
        }

        ShareLink link = new ShareLink();
        link.setToken(generateToken());
        link.setRemark(req.getRemark().trim());
        link.setVersionId(req.getVersionId());
        link.setExpireTime(req.getExpireTime());
        link.setMaxViews(req.getMaxViews());
        link.setViewCount(0);
        link.setEnabled(1);
        shareLinkMapper.insert(link);
        return link;
    }

    /**
     * 后台列表（最新生成在前）
     */
    public List<ShareLink> list() {
        return shareLinkMapper.selectList(
                new LambdaQueryWrapper<ShareLink>().orderByDesc(ShareLink::getId));
    }

    /**
     * 吊销链接（立即失效）
     */
    public void disable(Long id) {
        ShareLink link = shareLinkMapper.selectById(id);
        if (link == null) {
            throw new BusinessException(404, "链接不存在");
        }
        ShareLink update = new ShareLink();
        update.setId(id);
        update.setEnabled(0);
        shareLinkMapper.updateById(update);
    }

    /**
     * 256 位安全随机数 → 64 位十六进制串，不可枚举
     */
    private String generateToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }
}
