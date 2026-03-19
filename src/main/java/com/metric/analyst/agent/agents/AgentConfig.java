package com.metric.analyst.agent.agents;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Agent 配置类 - 基于 Spring AI Alibaba 官方最佳实践
 */
@Configuration
public class AgentConfig {

    @Value("${spring.ai.dashscope.api-key}")
    private String dashScopeApiKey;

    @Value("${spring.ai.dashscope.chat.options.model:qwen-turbo}")
    private String modelName;

    /**
     * 创建 ChatModel - DashScope (通义千问)
     */
    @Bean
    public ChatModel chatModel() {
        DashScopeApi dashScopeApi = DashScopeApi.builder()
                .apiKey(dashScopeApiKey)
                .build();

        return DashScopeChatModel.builder()
                .dashScopeApi(dashScopeApi)
                .build();
    }

    /**
     * 创建 MemorySaver - 用于状态管理
     */
    @Bean
    public MemorySaver memorySaver() {
        return new MemorySaver();
    }

    /**
     * 创建工具回调提供者
     */
    @Bean
    public MethodToolCallbackProvider metricQueryToolsProvider(MetricQueryTools metricQueryTools) {
        return MethodToolCallbackProvider.builder()
                .toolObjects(metricQueryTools)
                .build();
    }

    /**
     * 指标查询专家 Agent
     * 处理具体的数据查询（单指标、多地区对比、趋势、排名）
     */
    @Bean
    public ReactAgent metricQueryAgent(ChatModel chatModel, MemorySaver memorySaver,
                                       MethodToolCallbackProvider metricQueryToolsProvider) {
        ToolCallback[] tools = metricQueryToolsProvider.getToolCallbacks();
        return ReactAgent.builder()
                .name("metric_query_expert")
                .description("指标查询专家 - 处理具体的数据查询任务")
                .model(chatModel)
                .systemPrompt("""
                    你是【指标查询专家】，擅长处理人力资源指标数据查询。
                    
                    你的能力：
                    1. 查询单个指标在指定地区的当前值
                    2. 对比多个地区的同一指标
                    3. 查询指标的历史趋势
                    4. 查询指标的地区排名
                    
                    可用指标包括：招聘数量(recruitment_count)、平均薪资(avg_salary)等
                    支持的地区：北京、上海、广州、深圳、杭州、苏州、武汉、成都等
                    
                    请使用提供的工具来查询数据，并以友好、准确的方式回答用户。
                    """)
                .tools(tools)
                .saver(memorySaver)
                .build();
    }

    /**
     * 洞察分析专家 Agent
     * 处理深度分析（异常检测、关联分析、预测建议）
     */
    @Bean
    public ReactAgent insightAgent(ChatModel chatModel, MemorySaver memorySaver,
                                   MethodToolCallbackProvider metricQueryToolsProvider) {
        ToolCallback[] tools = metricQueryToolsProvider.getToolCallbacks();
        return ReactAgent.builder()
                .name("insight_analyst")
                .description("洞察分析专家 - 处理深度数据分析任务")
                .model(chatModel)
                .systemPrompt("""
                    你是【洞察分析专家】，擅长深度数据分析和业务洞察。
                    
                    你的能力：
                    1. 异常检测 - 识别数据中的异常点和趋势变化
                    2. 关联分析 - 发现不同指标之间的关系
                    3. 预测建议 - 基于历史数据提供未来趋势预测
                    4. 综合分析 - 生成多维度的分析报告
                    
                    你可以调用指标查询工具获取数据，然后基于数据提供专业的分析和建议。
                    分析时要结合业务背景，给出有洞察力的结论。
                    """)
                .tools(tools)
                .saver(memorySaver)
                .build();
    }

    /**
     * 维度解析专家 Agent
     * 处理用户输入中的维度信息提取
     */
    @Bean
    public ReactAgent dimensionParserAgent(ChatModel chatModel, MemorySaver memorySaver,
                                           MethodToolCallbackProvider metricQueryToolsProvider) {
        ToolCallback[] tools = metricQueryToolsProvider.getToolCallbacks();
        return ReactAgent.builder()
                .name("dimension_parser")
                .description("维度解析专家 - 从用户输入提取维度信息")
                .model(chatModel)
                .systemPrompt("""
                    你是【维度解析专家】，擅长从用户自然语言输入中提取结构化信息。
                    
                    你需要识别的维度：
                    1. 指标名称 - 如"招聘数量"、"平均薪资"等
                    2. 地区名称 - 如"北京"、"上海"、"杭州"等
                    3. 时间范围 - 如"今年"、"近6个月"、"2024年"等
                    4. 教育水平 - 如"本科"、"硕士"等
                    5. 公司类型 - 如"互联网"、"国企"等
                    
                    输出格式：将提取的信息整理成清晰的结构化数据。
                    """)
                .tools(tools)
                .saver(memorySaver)
                .build();
    }

    /**
     * 报告生成专家 Agent
     * 生成综合分析报告
     */
    @Bean
    public ReactAgent reportAgent(ChatModel chatModel, MemorySaver memorySaver,
                                  MethodToolCallbackProvider metricQueryToolsProvider) {
        ToolCallback[] tools = metricQueryToolsProvider.getToolCallbacks();
        return ReactAgent.builder()
                .name("report_generator")
                .description("报告生成专家 - 生成专业的数据分析报告")
                .model(chatModel)
                .systemPrompt("""
                    你是【报告生成专家】，擅长生成专业、清晰的数据分析报告。
                    
                    报告应包含：
                    1. 执行摘要 - 核心发现和建议
                    2. 数据概览 - 关键指标和数据展示
                    3. 详细分析 - 趋势、对比、异常等分析
                    4. 结论建议 - 基于数据的具体建议
                    
                    报告格式要专业，适合管理层阅读。
                    """)
                .tools(tools)
                .saver(memorySaver)
                .build();
    }
}
