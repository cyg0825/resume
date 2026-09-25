package com.example.resume.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.resume.common.BusinessException;
import com.example.resume.entity.Portfolio;
import com.example.resume.entity.Profile;
import com.example.resume.mapper.PortfolioMapper;
import com.example.resume.mapper.ProfileMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 作品集按“人（姓名）”管理，与简历版本无关：
 * - 同一人名下的所有简历版本共享同一份作品（前台按版本对应的姓名展示）
 * - 修改姓名不会搬运已有作品：新姓名视为另一个人，名下为空需重新添加
 * - 删除简历版本不影响任何作品
 */
@Service
@RequiredArgsConstructor
public class PortfolioService {

    private final PortfolioMapper portfolioMapper;
    private final ProfileMapper profileMapper;
    private final OwnerService ownerService;

    /**
     * 前台：按简历版本取对应姓名，返回该人名下共享的作品；姓名为空时返回空列表
     */
    public List<Portfolio> listByVersion(Long versionId) {
        String ownerName = resolveOwnerName(versionId);
        if (ownerName == null || ownerName.isBlank()) {
            return List.of();
        }
        return listByOwner(ownerName);
    }

    /** 后台：按归属人姓名列出作品 */
    public List<Portfolio> listByOwner(String ownerName) {
        if (ownerName == null || ownerName.isBlank()) {
            return List.of();
        }
        return portfolioMapper.selectList(new LambdaQueryWrapper<Portfolio>()
                .eq(Portfolio::getOwnerName, ownerName.trim())
                .orderByAsc(Portfolio::getSort)
                .orderByDesc(Portfolio::getId));
    }

    /** 归属人候选：作品集、荣誉证书等按人管理模块共用同一份候选列表 */
    public List<String> listOwners() {
        return ownerService.listOwners();
    }

    /** 新增作品：必须显式指定归属人姓名 */
    public void create(Portfolio portfolio) {
        String ownerName = normalizeOwner(portfolio.getOwnerName());
        if (ownerName == null) {
            throw new BusinessException(400, "请先选择或填写作品归属人姓名");
        }
        portfolio.setId(null);
        portfolio.setOwnerName(ownerName);
        portfolio.setCover(AssetVersionService.stripVersion(portfolio.getCover()));
        portfolioMapper.insert(portfolio);
    }

    /** 编辑作品：归属人不允许通过编辑接口变更 */
    public void update(Portfolio portfolio) {
        portfolio.setOwnerName(null);
        portfolio.setCover(AssetVersionService.stripVersion(portfolio.getCover()));
        portfolioMapper.updateById(portfolio);
    }

    public void delete(Long id) {
        portfolioMapper.deleteById(id);
    }

    private String normalizeOwner(String ownerName) {
        if (ownerName == null) {
            return null;
        }
        String trimmed = ownerName.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String resolveOwnerName(Long versionId) {
        if (versionId == null) {
            return null;
        }
        Profile profile = profileMapper.selectOne(new LambdaQueryWrapper<Profile>()
                .eq(Profile::getVersionId, versionId)
                .last("LIMIT 1"));
        return profile == null ? null : profile.getName();
    }
}
