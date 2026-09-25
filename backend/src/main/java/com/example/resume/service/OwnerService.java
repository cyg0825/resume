package com.example.resume.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.resume.entity.Honor;
import com.example.resume.entity.Portfolio;
import com.example.resume.entity.Profile;
import com.example.resume.mapper.HonorMapper;
import com.example.resume.mapper.PortfolioMapper;
import com.example.resume.mapper.ProfileMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * “归属人（姓名）”候选服务，供作品集、荣誉证书等按人管理的模块共用：
 * 已拥有作品/荣誉的人按最近维护时间排前，其余个人信息中出现过的姓名按名称补后。
 * 改名或删除版本后，历史内容仍能通过这里找到。
 */
@Service
@RequiredArgsConstructor
public class OwnerService {

    private final ProfileMapper profileMapper;
    private final PortfolioMapper portfolioMapper;
    private final HonorMapper honorMapper;

    public List<String> listOwners() {
        // 姓名 -> 最近一条内容的 id（作品、荣誉中取最大值），用于排序
        Map<String, Long> latestActivity = new LinkedHashMap<>();

        portfolioMapper.selectMaps(new QueryWrapper<Portfolio>()
                        .select("owner_name AS ownerName, MAX(id) AS latestId")
                        .groupBy("owner_name"))
                .forEach(row -> mergeActivity(latestActivity, row.get("ownerName"), row.get("latestId")));
        honorMapper.selectMaps(new QueryWrapper<Honor>()
                        .select("owner_name AS ownerName, MAX(id) AS latestId")
                        .groupBy("owner_name"))
                .forEach(row -> mergeActivity(latestActivity, row.get("ownerName"), row.get("latestId")));

        Set<String> owners = new LinkedHashSet<>();
        latestActivity.entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                .forEach(e -> owners.add(e.getKey()));

        // 仅有简历版本、还没有作品/荣誉的姓名，按名称排序补在后面
        profileMapper.selectList(new LambdaQueryWrapper<Profile>().select(Profile::getName))
                .stream()
                .map(p -> p.getName() == null ? null : p.getName().trim())
                .filter(n -> n != null && !n.isBlank() && !owners.contains(n))
                .distinct()
                .sorted()
                .forEach(owners::add);

        return List.copyOf(owners);
    }

    private void mergeActivity(Map<String, Long> map, Object nameObj, Object latestIdObj) {
        if (nameObj == null || nameObj.toString().isBlank()) {
            return;
        }
        String name = nameObj.toString().trim();
        long latestId = latestIdObj instanceof Number n ? n.longValue() : 0L;
        map.merge(name, latestId, Math::max);
    }
}
