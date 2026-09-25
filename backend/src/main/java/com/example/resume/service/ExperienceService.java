package com.example.resume.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.resume.entity.Experience;
import com.example.resume.mapper.ExperienceMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ExperienceService {

    private final ExperienceMapper experienceMapper;

    /** type：1 工作经历，2 项目经历；为空时查询全部 */
    public List<Experience> listByVersion(Long versionId, Integer type) {
        LambdaQueryWrapper<Experience> wrapper = new LambdaQueryWrapper<Experience>()
                .eq(Experience::getVersionId, versionId)
                .orderByAsc(Experience::getSort)
                .orderByDesc(Experience::getStartDate);
        if (type != null) {
            wrapper.eq(Experience::getType, type);
        }
        return experienceMapper.selectList(wrapper);
    }

    public void create(Experience experience) {
        experience.setId(null);
        experienceMapper.insert(experience);
    }

    public void update(Experience experience) {
        experienceMapper.updateById(experience);
    }

    public void delete(Long id) {
        experienceMapper.deleteById(id);
    }
}
