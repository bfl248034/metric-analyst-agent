package com.metric.analyst.agent.agents;

import com.metric.analyst.agent.config.ModelManager;
import com.metric.analyst.agent.skills.SkillTools;
import com.metric.analyst.agent.tools.MetricQueryTools;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

/**
 * 洞察分析智能体 - 专门处理深度数据分析和洞察
 */
@Component
public class InsightAgent {

    private final ModelManager modelManager;
    private final MetricQueryTools metricQueryTools;
    private final SkillTools skillTools;

    public InsightAgent(ModelManager modelManager,
                       MetricQueryTools metricQueryTools,
                       SkillTools skillTools) {
        this.modelManager = modelManager;
        this.metricQueryTools = metricQueryTools;
        this.skillTools = skillTools;
    }

    /**
     * 获取配置了工具的系统提示词 ChatClient
     */
    private ChatClient getChatClient() {
        return getChatClient(null);
    }

    /**
     * 获取配置了工具的系统提示词 ChatClient，指定模型
     */
    private ChatClient getChatClient(String provider) {
        ChatClient baseClient = provider != null 
            ? modelManager.getClient(provider) 
            : modelManager.getDefaultClient();
        
        // 添加系统提示词和工具
        return baseClient.mutate()
            .defaultSystem("""
                你是【数据洞察专家】，专门进行深度数据分析和洞察发现。
                
                你的职责：
                1. 识别数据中的异常和趋势
                2. 分析指标间的关联关系
                3. 提供有洞察力的结论和建议
                
                分析维度：
                - 异常检测：识别同比/环比异常波动的数据
                - 关联分析：分析多指标间的关系
                - 趋势预测：基于历史数据预测未来走势
                - 综合报告：生成多维度分析报告
                
                输出要求：
                - 结构化呈现（表格、列表）
                - 每个结论都有数据支撑
                - 给出可执行的建议
                - 使用 📊📈📉 等图标增强可读性
                """)
            .defaultTools(metricQueryTools, skillTools)
            .build();
    }

    public String handle(String request) {
        return handle(request, null);
    }

    public String handle(String request, String provider) {
        return getChatClient(provider).prompt()
            .user(request)
            .call()
            .content();
    }
}
