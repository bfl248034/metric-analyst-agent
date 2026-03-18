package com.metric.analyst.agent.config;

import com.alibaba.cloud.ai.agent.skill.FileSystemSkillRegistry;
import com.alibaba.cloud.ai.agent.skill.SkillRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Skills 配置
 */
@Slf4j
@Configuration
public class SkillsConfig {

    @Value("${skills.directory:src/main/resources/skills}")
    private String skillsDirectory;

    @Bean
    public SkillRegistry skillRegistry() {
        String path = System.getProperty("user.dir") + "/" + skillsDirectory;
        log.info("[SkillsConfig] Initializing SkillRegistry at: {}", path);
        
        return FileSystemSkillRegistry.builder()
            .projectSkillsDirectory(path)
            .build();
    }
}
