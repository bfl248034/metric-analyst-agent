package com.metric.analyst.agent.config;

import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.agent.flow.agent.SupervisorAgent;
import com.alibaba.cloud.ai.graph.agent.hook.skills.SkillsAgentHook;
import com.alibaba.cloud.ai.graph.agent.hook.skills.registry.SkillRegistry;
import com.alibaba.cloud.ai.graph.agent.hook.skills.registry.classpath.ClasspathSkillRegistry;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
import com.metric.analyst.agent.hooks.ChitchatHook;
import com.metric.analyst.agent.interceptor.PerformanceInterceptor;
import com.metric.analyst.agent.tools.MetricQueryTools;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * 多智能体配置类
 * 
 * 基于 Spring AI Alibaba 最佳实践配置：
 * - SupervisorAgent 作为主协调者
 * - ReactAgent 作为子 Agent
 * - Skills 渐进式知识披露
 * - Hooks 和 Interceptors 扩展能力
 */
@Slf4j
@Configuration
public class MultiAgentConfiguration {

    /**
     * Skill Registry - 从 classpath 加载 skills
     */
    @Bean
    public SkillRegistry skillRegistry() {
        return ClasspathSkillRegistry.builder()
            .classpathPath("skills")
            .build();
    }

    /**
     * Skills Agent Hook - 提供 read_skill 工具和技能列表注入
     */
    @Bean
    public SkillsAgentHook skillsAgentHook(SkillRegistry registry) {
        return SkillsAgentHook.builder()
            .skillRegistry(registry)
            .autoReload(true)  // 开发环境自动重载
            .build();
    }

    /**
     * Chitchat Hook - 闲聊识别与快速回复
     */
    @Bean
    public ChitchatHook chitchatHook() {
        return new ChitchatHook();
    }

    /**
     * Performance Interceptor - 性能监控
     */
    @Bean
    public PerformanceInterceptor performanceInterceptor() {
        return new PerformanceInterceptor();
    }

    // ==================== 子 Agent ====================

    /**
     * Metric Query Agent - 指标查询专家
     */
    @Bean
    public ReactAgent metricQueryAgent(ChatModel chatModel,
                                       MetricQueryTools queryTools,
                                       SkillsAgentHook skillsHook,
                                       ChitchatHook chitchatHook,
                                       PerformanceInterceptor performanceInterceptor) {
        
        return ReactAgent.builder()
            .name("metric-query-agent")
            .model(chatModel)
            .description("专门处理指标数据查询，支持当前值、趋势、排名、对比")
            .instruction("""
                你是指标查询专家。请按以下步骤处理用户请求：
                
                1. 识别用户想查询的指标和维度（地区/时间/学历等）
                2. 使用 normalizeDimensions 标准化维度值
                3. 根据查询类型选择合适的工具：
                   - 单值查询 → queryCurrentValue
                   - 趋势查询 → queryTrend
                   - 排名查询 → queryRanking
                   - 对比查询 → queryComparison
                4. 返回结构化的数据结果
                
                可用指标：招聘薪资、岗位数量、企业数量、专利申请等
                """)
            .tools(queryTools)
            .hooks(List.of(skillsHook, chitchatHook))
            .interceptors(performanceInterceptor)
            .saver(new MemorySaver())
            .build();
    }

    /**
     * Insight Analysis Agent - 洞察分析专家
     */
    @Bean
    public ReactAgent insightAnalysisAgent(ChatModel chatModel,
                                           MetricQueryTools queryTools,
                                           SkillsAgentHook skillsHook) {
        
        return ReactAgent.builder()
            .name("insight-analysis-agent")
            .model(chatModel)
            .description("基于查询结果进行深度洞察分析")
            .instruction("""
                你是数据分析专家。基于查询结果提供深度洞察：
                
                1. 分析数据的趋势和规律
                2. 识别异常和亮点
                3. 进行多维度对比分析
                4. 提供业务见解和建议
                
                使用工具获取所需数据，然后进行分析解读。
                """)
            .tools(queryTools)
            .hooks(List.of(skillsHook))
            .saver(new MemorySaver())
            .build();
    }

    /**
     * Report Generation Agent - 报告生成专家
     */
    @Bean
    public ReactAgent reportGenerationAgent(ChatModel chatModel,
                                            MetricQueryTools queryTools,
                                            SkillsAgentHook skillsHook) {
        
        return ReactAgent.builder()
            .name("report-generation-agent")
            .model(chatModel)
            .description("生成专业的数据分析报告")
            .instruction("""
                你是报告生成专家。根据用户需求生成专业报告：
                
                1. 收集相关数据（多指标、多维度）
                2. 组织报告结构（摘要、正文、结论）
                3. 生成可视化建议
                4. 输出格式化的报告内容
                
                报告应包含数据摘要、趋势分析、对比结论。
                """)
            .tools(queryTools)
            .hooks(List.of(skillsHook))
            .saver(new MemorySaver())
            .build();
    }

    // ==================== 主 SupervisorAgent ====================

    /**
     * Supervisor Agent - 主协调者
     * 
     * 负责：
     * 1. 意图识别（闲聊/query/analysis/report）
     * 2. 任务路由到子 Agent
     * 3. 多步骤任务编排
     */
    @Bean
    public SupervisorAgent supervisorAgent(ChatModel chatModel,
                                           SkillsAgentHook skillsHook,
                                           ChitchatHook chitchatHook,
                                           ReactAgent metricQueryAgent,
                                           ReactAgent insightAnalysisAgent,
                                           ReactAgent reportGenerationAgent) {
        
        return SupervisorAgent.builder()
            .name("metric-analyst-supervisor")
            .description("Metric Analyst 主协调者，负责任务分发和编排")
            .model(chatModel)
            .systemPrompt("""
                你是 Metric Analyst 的主协调者。
                
                ## 职责
                1. 分析用户输入，识别意图类型
                2. 将任务路由到最合适的子 Agent
                3. 协调多步骤任务执行
                
                ## 意图分类
                - chitchat: 打招呼、询问身份、无关话题 → 直接友好回复
                - query: 具体数据查询 → 路由到 metric-query-agent
                - analysis: 深度分析、对比、异常检测 → 路由到 insight-analysis-agent  
                - report: 报告生成 → 路由到 report-generation-agent
                
                ## 可用子 Agent
                - metric-query-agent: 指标查询专家，处理数据查询
                - insight-analysis-agent: 洞察分析专家，处理深度分析
                - report-generation-agent: 报告生成专家，生成专业报告
                
                ## 响应规则
                只返回 Agent 名称或 FINISH，不要解释：
                - metric-query-agent
                - insight-analysis-agent
                - report-generation-agent
                - FINISH
                """)
            .subAgents(List.of(metricQueryAgent, insightAnalysisAgent, reportGenerationAgent))
            .hooks(List.of(skillsHook, chitchatHook))
            .build();
    }
}
