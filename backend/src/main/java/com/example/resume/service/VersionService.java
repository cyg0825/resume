package com.example.resume.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.example.resume.common.BusinessException;
import com.example.resume.dto.VersionCreateRequest;
import com.example.resume.entity.*;
import com.example.resume.mapper.*;
import com.example.resume.security.SharePrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

/**
 * 简历版本管理：
 * 版本切换、创建（支持从已有版本克隆全部内容）、删除、默认版本解析
 */
@Service
@RequiredArgsConstructor
public class VersionService {

    private final ResumeVersionMapper versionMapper;
    private final ProfileMapper profileMapper;
    private final EducationMapper educationMapper;
    private final ExperienceMapper experienceMapper;
    private final SkillMapper skillMapper;
    // 作品集与荣誉证书按姓名归档、与版本无关：不随版本复制，删除版本也不删除

    /**
     * 解析当前请求应使用的版本 ID（服务端强制版本隔离的唯一入口）。
     *
     * - 管理员：指定 versionId 则校验存在后使用，否则取默认版本，再退回 ID=1；
     * - 访客（专属链接）：前端传入的 versionId 一律不可信——
     *   · 链接绑定了版本：只能看该版本，忽略任何请求参数；
     *   · 链接未绑定（跟随默认）：只能看当前默认版本；
     *   以此防止访客篡改 versionId 越权读取其他简历版本。
     */
    public Long resolveVersionId(Long versionId) {
        SharePrincipal visitor = currentVisitor();
        if (visitor != null) {
            Long bound = visitor.versionId();
            if (bound == null) {
                // 链接未绑定版本（跟随默认）：强制使用当前默认版本，忽略请求参数
                return defaultVersionId();
            }
            // 链接绑定了版本：只能看该版本；版本已被删除时给出明确错误
            if (versionMapper.selectById(bound) == null) {
                throw new BusinessException(404, "链接绑定的简历版本已不可用，请联系站长更新链接");
            }
            return bound;
        }

        if (versionId != null) {
            if (versionMapper.selectById(versionId) == null) {
                throw new BusinessException(404, "指定的简历版本不存在");
            }
            return versionId;
        }
        return defaultVersionId();
    }

    /** 当前认证主体为访客时返回其专属链接主体，否则（管理员/无认证）返回 null */
    private SharePrincipal currentVisitor() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof SharePrincipal sharePrincipal) {
            return sharePrincipal;
        }
        return null;
    }

    /**
     * 解析默认版本 ID：取默认标记版本，多个时选内容最完整的，无默认标记时退回 ID=1。
     * 若存在多个默认标记（历史数据异常），选择简历内容最完整的那个，
     * 避免命中只有基本信息的空壳版本导致 AI 问答无内容可用。
     */
    private Long defaultVersionId() {
        List<ResumeVersion> defaults = versionMapper.selectList(
                new LambdaQueryWrapper<ResumeVersion>().eq(ResumeVersion::getIsDefault, 1));
        if (defaults.size() == 1) {
            return defaults.get(0).getId();
        }
        if (defaults.size() > 1) {
            return defaults.stream()
                    .max(Comparator.comparingLong(this::contentScore)
                            .thenComparing(ResumeVersion::getId))
                    .map(ResumeVersion::getId)
                    .orElse(defaults.get(0).getId());
        }
        ResumeVersion first = versionMapper.selectById(1L);
        if (first == null) {
            throw new BusinessException(404, "尚未初始化任何简历版本");
        }
        return first.getId();
    }

    /** 版本内容完整度评分：教育 + 经历 + 技能三表记录数之和，作品集/荣誉按姓名归档不计入 */
    private long contentScore(ResumeVersion version) {
        Long id = version.getId();
        return safeCount(educationMapper.selectCount(
                        new LambdaQueryWrapper<Education>().eq(Education::getVersionId, id)))
                + safeCount(experienceMapper.selectCount(
                        new LambdaQueryWrapper<Experience>().eq(Experience::getVersionId, id)))
                + safeCount(skillMapper.selectCount(
                        new LambdaQueryWrapper<Skill>().eq(Skill::getVersionId, id)));
        // 作品集、荣誉证书按姓名归档，不计入版本内容完整度
    }

    private long safeCount(Long count) {
        return count == null ? 0 : count;
    }

    public List<ResumeVersion> list() {
        return versionMapper.selectList(
                new LambdaQueryWrapper<ResumeVersion>().orderByDesc(ResumeVersion::getCreateTime));
    }

    /**
     * 创建新版本；sourceVersionId 非空时克隆源版本下的所有内容
     */
    @Transactional(rollbackFor = Exception.class)
    public ResumeVersion create(VersionCreateRequest request) {
        ResumeVersion version = new ResumeVersion();
        version.setVersionName(request.getVersionName());
        version.setDescription(request.getDescription());
        // 非默认版本存 NULL（表级 UNIQUE(is_default) 保证最多一个 1，NULL 不参与唯一性判定；
        // 索引名由 H2 自动生成，DDL 里没有显式命名）
        version.setIsDefault(null);
        versionMapper.insert(version);

        Long sourceId = request.getSourceVersionId();
        if (sourceId != null) {
            copyContents(sourceId, version.getId());
        } else {
            // 为新版本初始化一条空白个人信息，保证前台 /api/profile 必有数据
            Profile profile = new Profile();
            profile.setVersionId(version.getId());
            profileMapper.insert(profile);
        }

        // 第一个版本自动设为默认
        if (versionMapper.selectCount(null) == 1) {
            setDefault(version.getId());
        }
        return version;
    }

    private void copyContents(Long sourceId, Long targetId) {
        Profile sourceProfile = profileMapper.selectOne(
                new LambdaQueryWrapper<Profile>().eq(Profile::getVersionId, sourceId));
        if (sourceProfile != null) {
            sourceProfile.setId(null);
            sourceProfile.setVersionId(targetId);
            profileMapper.insert(sourceProfile);
        } else {
            Profile profile = new Profile();
            profile.setVersionId(targetId);
            profileMapper.insert(profile);
        }

        for (Education item : educationMapper.selectList(
                new LambdaQueryWrapper<Education>().eq(Education::getVersionId, sourceId))) {
            item.setId(null);
            item.setVersionId(targetId);
            educationMapper.insert(item);
        }
        for (Experience item : experienceMapper.selectList(
                new LambdaQueryWrapper<Experience>().eq(Experience::getVersionId, sourceId))) {
            item.setId(null);
            item.setVersionId(targetId);
            experienceMapper.insert(item);
        }
        for (Skill item : skillMapper.selectList(
                new LambdaQueryWrapper<Skill>().eq(Skill::getVersionId, sourceId))) {
            item.setId(null);
            item.setVersionId(targetId);
            skillMapper.insert(item);
        }
        // 作品集、荣誉证书按姓名归档、与版本无关：克隆版本若姓名相同则天然共享，无需复制
    }

    /**
     * 一键切换默认展示版本
     */
    @Transactional(rollbackFor = Exception.class)
    public void setDefault(Long id) {
        ResumeVersion version = versionMapper.selectById(id);
        if (version == null) {
            throw new BusinessException(404, "版本不存在");
        }
        // 先把原默认版本置为 NULL（唯一索引只允许一个 1，NULL 可多行共存）。
        // 必须用 UpdateWrapper.set 显式赋值，MyBatis-Plus 默认会忽略实体中的 null 字段。
        versionMapper.update(null, new LambdaUpdateWrapper<ResumeVersion>()
                .eq(ResumeVersion::getIsDefault, 1)
                .set(ResumeVersion::getIsDefault, null));

        version.setIsDefault(1);
        versionMapper.updateById(version);
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        ResumeVersion version = versionMapper.selectById(id);
        if (version == null) {
            throw new BusinessException(404, "版本不存在");
        }
        if (Integer.valueOf(1).equals(version.getIsDefault())) {
            throw new BusinessException(400, "默认展示版本不可删除，请先切换默认版本");
        }

        educationMapper.delete(new LambdaQueryWrapper<Education>().eq(Education::getVersionId, id));
        experienceMapper.delete(new LambdaQueryWrapper<Experience>().eq(Experience::getVersionId, id));
        skillMapper.delete(new LambdaQueryWrapper<Skill>().eq(Skill::getVersionId, id));
        // 作品集、荣誉证书按姓名归档、与版本无关，删除版本不影响其内容
        profileMapper.delete(new LambdaQueryWrapper<Profile>().eq(Profile::getVersionId, id));
        versionMapper.deleteById(id);
    }

    public void update(ResumeVersion version) {
        if (version.getId() == null || versionMapper.selectById(version.getId()) == null) {
            throw new BusinessException(404, "版本不存在");
        }
        ResumeVersion update = new ResumeVersion();
        update.setId(version.getId());
        update.setVersionName(version.getVersionName());
        update.setDescription(version.getDescription());
        versionMapper.updateById(update);
    }
}
