package com.example.resume.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.resume.entity.Profile;
import com.example.resume.mapper.ProfileMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProfileService {

    private final ProfileMapper profileMapper;

    /** 按版本查询个人信息，不存在时返回空对象（方便前台渲染与后台编辑） */
    public Profile getByVersion(Long versionId) {
        Profile profile = profileMapper.selectOne(new LambdaQueryWrapper<Profile>()
                .eq(Profile::getVersionId, versionId)
                .last("LIMIT 1"));
        return profile != null ? profile : emptyProfile(versionId);
    }

    /**
     * 新增或更新某版本的个人信息（每个版本仅一条）。
     * 注意：修改姓名不会迁移作品集——作品集按姓名归档，改名即视为另一个人，
     * 新姓名名下的作品需要在作品集管理中重新添加。
     */
    public Profile saveByVersion(Profile param) {
        // 出参图片 URL 带的 ?v= 指纹参数不能写回数据库
        param.setAvatar(AssetVersionService.stripVersion(param.getAvatar()));
        Profile existing = profileMapper.selectOne(new LambdaQueryWrapper<Profile>()
                .eq(Profile::getVersionId, param.getVersionId())
                .last("LIMIT 1"));
        if (existing == null) {
            param.setId(null);
            profileMapper.insert(param);
            return param;
        }
        param.setId(existing.getId());
        profileMapper.updateById(param);
        return param;
    }

    private Profile emptyProfile(Long versionId) {
        Profile profile = new Profile();
        profile.setVersionId(versionId);
        return profile;
    }
}
