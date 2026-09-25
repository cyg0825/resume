package com.example.resume.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.resume.entity.Skill;
import com.example.resume.mapper.SkillMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SkillService {

    private final SkillMapper skillMapper;

    public List<Skill> listByVersion(Long versionId) {
        return skillMapper.selectList(new LambdaQueryWrapper<Skill>()
                .eq(Skill::getVersionId, versionId)
                .orderByAsc(Skill::getSort)
                .orderByAsc(Skill::getId));
    }

    public void create(Skill skill) {
        skill.setId(null);
        skillMapper.insert(skill);
    }

    public void update(Skill skill) {
        skillMapper.updateById(skill);
    }

    public void delete(Long id) {
        skillMapper.deleteById(id);
    }
}
