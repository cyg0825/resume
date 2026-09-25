package com.example.resume.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.resume.entity.Education;
import com.example.resume.mapper.EducationMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EducationService {

    private final EducationMapper educationMapper;

    public List<Education> listByVersion(Long versionId) {
        return educationMapper.selectList(new LambdaQueryWrapper<Education>()
                .eq(Education::getVersionId, versionId)
                .orderByAsc(Education::getSort)
                .orderByDesc(Education::getStartDate));
    }

    public void create(Education education) {
        education.setId(null);
        educationMapper.insert(education);
    }

    public void update(Education education) {
        educationMapper.updateById(education);
    }

    public void delete(Long id) {
        educationMapper.deleteById(id);
    }
}
