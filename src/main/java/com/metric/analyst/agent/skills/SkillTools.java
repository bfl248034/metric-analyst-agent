package com.metric.analyst.agent.skills;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

/**
 * 技能调用工具 - 供 LLM 调用
 */
@Component
public class SkillTools {

    private final SkillRegistry skillRegistry;

    public SkillTools(SkillRegistry skillRegistry) {
        this.skillRegistry = skillRegistry;
    }

    /**
     * 列出所有可用技能
     */
    @Tool(name = "listSkills", description = "列出所有可用的技能及其描述")
    public String listSkills() {
        return skillRegistry.getSkillDescriptions();
    }

    /**
     * 获取技能详情
     */
    @Tool(name = "getSkillDetail", description = "获取指定技能的完整说明文档")
    public String getSkillDetail(
            @ToolParam(description = "技能名称，如 metric-analysis、data-insight") String skillName) {
        Skill skill = skillRegistry.getSkill(skillName);
        if (skill == null) {
            return "未找到技能: " + skillName + "\n可用技能:\n" + skillRegistry.getSkillDescriptions();
        }
        return skill.getContent();
    }

    /**
     * 匹配最适合的技能
     */
    @Tool(name = "matchSkill", description = "根据用户输入匹配最适合的技能")
    public String matchSkill(
            @ToolParam(description = "用户输入的问题") String userInput) {
        Skill skill = skillRegistry.matchSkill(userInput);
        if (skill == null) {
            return "未匹配到合适的技能";
        }
        return "匹配到技能: " + skill.getName() + "\n描述: " + skill.getDescription() + 
               "\n\n完整说明:\n" + skill.getContent();
    }
}
