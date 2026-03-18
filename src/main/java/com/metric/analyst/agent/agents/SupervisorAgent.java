package com.metric.analyst.agent.agents;

import com.metric.analyst.agent.skills.SkillRegistry;
import com.metric.analyst.agent.skills.SkillTools;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

/**
 * 主管智能体 - Supervisor 模式，协调多个子智能体
 */
@Component
public class SupervisorAgent {

    private final ChatClient chatClient;
    private final MetricQueryAgent metricQueryAgent;
    private final InsightAgent insightAgent;
    private final SkillRegistry skillRegistry;

    public SupervisorAgent(ChatClient.Builder chatClientBuilder,
                          MetricQueryAgent metricQueryAgent,
                          InsightAgent insightAgent,
                          SkillRegistry skillRegistry,
                          SkillTools skillTools) {
        this.metricQueryAgent = metricQueryAgent;
        this.insightAgent = insightAgent;
        this.skillRegistry = skillRegistry;
        
        this.chatClient = chatClientBuilder
            .defaultSystem("""
                你是【主管智能体】，负责协调指标查询专家和洞察分析专家完成用户请求。
                
                你的团队：
                1. 【指标查询专家】- 处理具体的数据查询（单指标、多地区对比、趋势、排名）
                2. 【洞察分析专家】- 处理深度分析（异常检测、关联分析、预测建议）
                
                工作流程：
                1. 分析用户请求类型
                2. 决定分配给哪个专家处理
                3. 如有需要，协调多个专家协作
                4. 整合输出，确保结果完整准确
                
                分配规则：
                - 简单查询（具体数值、排名、对比）→ 指标查询专家
                - 深度分析（异常、关联、预测、报告）→ 洞察分析专家
                - 复合请求 → 先查询后分析，协调两者协作
                
                你可以使用技能系统了解详细用法。
                """)
            .defaultTools(skillTools)
            .build();
    }

    /**
     * 处理用户请求 - 智能路由到合适的子智能体
     */
    public String handle(String userInput) {
        // 1. 判断请求类型
        RequestType type = classifyRequest(userInput);
        
        switch (type) {
            case SIMPLE_QUERY:
                // 简单查询交给指标查询专家
                return metricQueryAgent.handle(userInput);
                
            case DEEP_ANALYSIS:
                // 深度分析交给洞察分析专家
                return insightAgent.handle(userInput);
                
            case COMPOSITE:
            default:
                // 复合请求：先查询后分析
                String queryResult = metricQueryAgent.handle(userInput);
                String analysisRequest = "基于以下数据进行分析:\n" + queryResult + 
                                        "\n\n用户原始问题: " + userInput;
                return insightAgent.handle(analysisRequest);
        }
    }

    /**
     * 分类请求类型
     */
    private RequestType classifyRequest(String input) {
        String lower = input.toLowerCase();
        
        // 深度分析关键词
        if (containsAny(lower, "分析", "洞察", "异常", "关联", "预测", "报告", "建议", "为什么", "原因")) {
            return RequestType.DEEP_ANALYSIS;
        }
        
        // 简单查询关键词
        if (containsAny(lower, "多少", "排名", "对比", "趋势", "查询", "查一下")) {
            return RequestType.SIMPLE_QUERY;
        }
        
        // 复合请求（包含多个意图）
        if (lower.length() > 20 && containsAny(lower, "和", "并且", "同时")) {
            return RequestType.COMPOSITE;
        }
        
        return RequestType.SIMPLE_QUERY;
    }

    private boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    enum RequestType {
        SIMPLE_QUERY,    // 简单查询
        DEEP_ANALYSIS,   // 深度分析
        COMPOSITE        // 复合请求
    }
}
