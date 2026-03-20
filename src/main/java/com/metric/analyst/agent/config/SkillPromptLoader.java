package com.metric.analyst.agent.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Skills Prompt 加载器
 * 
 * 负责加载 docs/skills/ 目录下的 Markdown 文件，
 * 解析并组合成 System Prompt，供大模型理解系统能力。
 */
@Slf4j
@Component
public class SkillPromptLoader {

    @Value("${skills.prompt.path:docs/skills/*.md}")
    private String skillsPath;
    
    @Value("${skills.prompt.enabled:true}")
    private boolean enabled;

    private final Map<String, String> skillDescriptions = new LinkedHashMap<>();
    private String systemPrompt;
    
    private final PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();

    @PostConstruct
    public void load() {
        if (!enabled) {
            log.info("Skills prompt loading is disabled");
            return;
        }
        
        try {
            log.info("Loading skills from: {}", skillsPath);
            
            Resource[] resources = resolver.getResources("classpath:" + skillsPath);
            
            for (Resource resource : resources) {
                if (resource.exists() && resource.isReadable()) {
                    String content = readResource(resource);
                    String skillName = extractSkillName(resource.getFilename());
                    skillDescriptions.put(skillName, content);
                    log.info("Loaded skill: {}", skillName);
                }
            }
            
            // 构建 System Prompt
            buildSystemPrompt();
            
            log.info("Successfully loaded {} skills", skillDescriptions.size());
            
        } catch (IOException e) {
            log.error("Failed to load skills", e);
        }
    }

    /**
     * 读取资源文件内容
     */
    private String readResource(Resource resource) throws IOException {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
            return reader.lines().collect(Collectors.joining("\n"));
        }
    }

    /**
     * 从文件名提取 Skill 名称
     */
    private String extractSkillName(String filename) {
        if (filename == null) return "unknown";
        // 移除 .md 后缀
        return filename.replace(".md", "");
    }

    /**
     * 构建完整的 System Prompt
     */
    private void buildSystemPrompt() {
        StringBuilder sb = new StringBuilder();
        
        // 基础角色设定
        sb.append("# Metric Analyst Agent\n\n");
        sb.append("你是 Metric Analyst（数据分析智能体），一个专业的经济数据查询助手。\n\n");
        
        // 核心能力概述
        sb.append("## 核心能力\n\n");
        sb.append("你可以帮助用户查询和分析以下领域的数据：\n");
        sb.append("- 📊 招聘就业：岗位数量、平均薪酬、招聘企业数\n");
        sb.append("- 🏢 市场主体：新增、注销、在营企业数量\n");
        sb.append("- 💡 知识产权：专利申请数量\n");
        sb.append("- 🛒 政府采购：金额、数量、均价\n\n");
        
        // 工作方式说明
        sb.append("## 工作方式\n\n");
        sb.append("你的处理流程分为以下阶段：\n\n");
        sb.append("1. **闲聊识别**: 判断用户是否在打招呼或问无关问题\n");
        sb.append("2. **指标识别**: 理解用户想查询什么指标\n");
        sb.append("3. **维度标准化**: 将用户输入的地区、时间等转换为系统编码\n");
        sb.append("4. **数据查询**: 调用对应工具查询数据\n");
        sb.append("5. **结果生成**: 整合数据，生成自然语言回复\n\n");
        
        // 可用 Skills
        sb.append("## 可用 Skills\n\n");
        
        for (Map.Entry<String, String> entry : skillDescriptions.entrySet()) {
            String skillDesc = extractSkillSummary(entry.getValue());
            sb.append(skillDesc).append("\n\n");
        }
        
        // 回复风格指引
        sb.append("## 回复风格\n\n");
        sb.append("1. **简洁明了**: 先说核心数据，再补充细节\n");
        sb.append("2. **数据准确**: 使用查询返回的确切数值\n");
        sb.append("3. **自然流畅**: 用口语化的方式描述数据\n");
        sb.append("4. **引导清晰**: 当无法识别意图时，给出具体示例\n\n");
        
        // 边界说明
        sb.append("## 能力边界\n\n");
        sb.append("✅ 你能做的：\n");
        sb.append("- 查询10个核心指标的数值、趋势、排名\n");
        sb.append("- 支持多地区对比分析\n");
        sb.append("- 识别地区别称（如"帝都"="北京"）\n\n");
        sb.append("❌ 你不能做的：\n");
        sb.append("- 查询指标范围外的数据（如GDP、人口等）\n");
        sb.append("- 进行复杂的跨指标关联分析\n");
        sb.append("- 预测未来数据\n\n");
        
        this.systemPrompt = sb.toString();
    }

    /**
     * 提取 Skill Markdown 的摘要部分
     */
    private String extractSkillSummary(String markdown) {
        StringBuilder summary = new StringBuilder();
        String[] lines = markdown.split("\n");
        
        boolean inBasicInfo = false;
        boolean inTools = false;
        
        for (String line : lines) {
            // 提取基本信息部分
            if (line.startsWith("# ")) {
                summary.append("### ").append(line.substring(2)).append("\n");
            }
            
            if (line.startsWith("- **Skill ID**")) {
                summary.append(line).append("\n");
            }
            
            if (line.startsWith("- **功能**")) {
                summary.append(line).append("\n");
            }
            
            // 提取能力范围
            if (line.startsWith("## 能力范围")) {
                inBasicInfo = true;
                continue;
            }
            
            if (inBasicInfo && line.startsWith("## ")) {
                inBasicInfo = false;
            }
            
            if (inBasicInfo && !line.trim().isEmpty()) {
                summary.append(line).append("\n");
            }
            
            // 提取可用工具
            if (line.startsWith("## 可用工具")) {
                summary.append("\n**可用工具**:\n");
                inTools = true;
                continue;
            }
            
            if (inTools && line.startsWith("### ")) {
                summary.append("- ").append(line.substring(4)).append("\n");
            }
            
            if (inTools && line.startsWith("## ") && !line.contains("可用工具")) {
                inTools = false;
            }
        }
        
        return summary.toString();
    }

    /**
     * 获取完整的 System Prompt
     */
    public String getSystemPrompt() {
        return systemPrompt;
    }

    /**
     * 获取指定 Skill 的完整描述
     */
    public String getSkillDescription(String skillName) {
        return skillDescriptions.get(skillName);
    }

    /**
     * 获取所有 Skill 名称
     */
    public Set<String> getSkillNames() {
        return skillDescriptions.keySet();
    }

    /**
     * 获取所有 Skill 描述
     */
    public Map<String, String> getAllSkillDescriptions() {
        return new LinkedHashMap<>(skillDescriptions);
    }

    /**
     * 构建针对特定 Skills 的精简 Prompt
     * 用于 Agent 只需要部分 Skills 的场景
     */
    public String buildTargetedPrompt(List<String> targetSkills) {
        StringBuilder sb = new StringBuilder();
        
        sb.append("# Metric Analyst Agent\n\n");
        sb.append("你是数据分析智能体。\n\n");
        sb.append("当前可用 Skills:\n\n");
        
        for (String skillName : targetSkills) {
            String desc = skillDescriptions.get(skillName);
            if (desc != null) {
                sb.append(desc).append("\n\n");
            }
        }
        
        return sb.toString();
    }
}
