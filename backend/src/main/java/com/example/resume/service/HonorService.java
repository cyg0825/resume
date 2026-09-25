package com.example.resume.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.resume.common.BusinessException;
import com.example.resume.entity.Honor;
import com.example.resume.entity.Profile;
import com.example.resume.mapper.HonorMapper;
import com.example.resume.mapper.ProfileMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 荣誉证书按“人（姓名）”管理，规则与作品集一致：
 * 同名版本共享、改名即新人（不迁移）、删除版本不删除荣誉。
 */
@Service
@RequiredArgsConstructor
public class HonorService {

    private final HonorMapper honorMapper;
    private final ProfileMapper profileMapper;

    /** 前台：按简历版本取对应姓名，返回该人名下共享的荣誉 */
    public List<Honor> listByVersion(Long versionId) {
        String ownerName = resolveOwnerName(versionId);
        if (ownerName == null || ownerName.isBlank()) {
            return List.of();
        }
        return listByOwner(ownerName);
    }

    /** 后台：按归属人姓名列出荣誉 */
    public List<Honor> listByOwner(String ownerName) {
        if (ownerName == null || ownerName.isBlank()) {
            return List.of();
        }
        return honorMapper.selectList(new LambdaQueryWrapper<Honor>()
                .eq(Honor::getOwnerName, ownerName.trim())
                .orderByAsc(Honor::getSort)
                .orderByAsc(Honor::getId));
    }

    /** 新增荣誉：必须显式指定归属人姓名 */
    public Honor create(Honor honor) {
        String ownerName = normalizeOwner(honor.getOwnerName());
        if (ownerName == null) {
            throw new BusinessException(400, "请先选择或填写荣誉归属人姓名");
        }
        honor.setId(null);
        honor.setOwnerName(ownerName);
        honor.setImage(AssetVersionService.stripVersion(honor.getImage()));
        honorMapper.insert(honor);
        return honor;
    }

    /** 编辑荣誉：归属人不允许通过编辑接口变更 */
    public void update(Honor honor) {
        honor.setOwnerName(null);
        honor.setImage(AssetVersionService.stripVersion(honor.getImage()));
        honorMapper.updateById(honor);
    }

    public void delete(Long id) {
        honorMapper.deleteById(id);
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
