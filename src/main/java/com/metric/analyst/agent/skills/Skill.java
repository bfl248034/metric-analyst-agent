package com.metric.analyst.agent.skills;

import lombok.Data;
import java.util.List;
import java.util.Map;

/**
 * 技能定义
 */
@Data
public class Skill {
    private String name;
    private String description;
    private String author;
    private String version;
    private String content;
    private String path;
    private List<String> references;
    
    /**
     * 从 SKILL.md 内容解析技能
     */
    public static Skill parse(String content, String path) {
        Skill skill = new Skill();
        skill.setPath(path);
        skill.setContent(content);
        
        // 解析 YAML frontmatter
        if (content.startsWith("---")) {
            int endIndex = content.indexOf("---", 3);
            if (endIndex != -1) {
                String frontmatter = content.substring(3, endIndex).trim();
                Map<String, String> metadata = parseFrontmatter(frontmatter);
                skill.setName(metadata.get("name"));
                skill.setDescription(metadata.get("description"));
                skill.setAuthor(metadata.get("author"));
                skill.setVersion(metadata.get("version"));
            }
        }
        
        return skill;
    }
    
    private static Map<String, String> parseFrontmatter(String frontmatter) {
        Map<String, String> map = new java.util.HashMap<>();
        String[] lines = frontmatter.split("\n");
        for (String line : lines) {
            if (line.contains(":")) {
                int colonIndex = line.indexOf(":");
                String key = line.substring(0, colonIndex).trim();
                String value = line.substring(colonIndex + 1).trim();
                map.put(key, value);
            }
        }
        return map;
    }
}
