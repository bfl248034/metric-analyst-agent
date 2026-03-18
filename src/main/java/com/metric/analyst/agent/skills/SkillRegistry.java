package com.metric.analyst.agent.skills;

import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.*;

/**
 * 技能注册中心 - 扫描和管理所有 Skills
 */
@Component
public class SkillRegistry {

    private final Map<String, Skill> skills = new HashMap<>();
    private final PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();

    @PostConstruct
    public void init() {
        scanSkills();
    }

    /**
     * 扫描 classpath 下的所有技能
     */
    public void scanSkills() {
        try {
            // 扫描 skills 目录下的所有 SKILL.md
            Resource[] resources = resolver.getResources("classpath:skills/**/SKILL.md");
            
            for (Resource resource : resources) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(resource.getInputStream()))) {
                    StringBuilder content = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        content.append(line).append("\n");
                    }
                    
                    String path = resource.getURI().toString();
                    Skill skill = Skill.parse(content.toString(), path);
                    
                    if (skill.getName() != null) {
                        skills.put(skill.getName(), skill);
                        System.out.println("[SkillRegistry] Loaded skill: " + skill.getName() + " - " + skill.getDescription());
                    }
                }
            }
            
            System.out.println("[SkillRegistry] Total skills loaded: " + skills.size());
            
        } catch (Exception e) {
            System.err.println("[SkillRegistry] Failed to scan skills: " + e.getMessage());
        }
    }

    /**
     * 根据名称获取技能
     */
    public Skill getSkill(String name) {
        return skills.get(name);
    }

    /**
     * 获取所有技能
     */
    public Collection<Skill> getAllSkills() {
        return skills.values();
    }

    /**
     * 获取技能描述列表（用于 LLM 判断）
     */
    public String getSkillDescriptions() {
        StringBuilder sb = new StringBuilder();
        sb.append("可用技能列表:\n");
        for (Skill skill : skills.values()) {
            sb.append("- ").append(skill.getName()).append(": ").append(skill.getDescription()).append("\n");
        }
        return sb.toString();
    }

    /**
     * 匹配最适合的技能
     */
    public Skill matchSkill(String userInput) {
        // 简单匹配：根据关键词匹配技能
        String lower = userInput.toLowerCase();
        
        for (Skill skill : skills.values()) {
            String desc = skill.getDescription().toLowerCase();
            // 计算匹配度
            int matchCount = 0;
            for (String word : lower.split("")) {
                if (word.trim().length() > 1 && desc.contains(word)) {
                    matchCount++;
                }
            }
            if (matchCount > 2) {
                return skill;
            }
        }
        
        // 默认返回指标分析技能
        return skills.get("metric-analysis");
    }
}
