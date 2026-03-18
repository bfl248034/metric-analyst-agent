package com.metric.analyst.agent.agents;

import com.metric.analyst.agent.skills.SkillRegistry;
import com.metric.analyst.agent.skills.SkillTools;
import com.metric.analyst.agent.tools.MetricQueryTools;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

/**
 * 指标查询智能体 - 专门处理指标数据查询
 */
@Component
public class MetricQueryAgent {

    private final ChatClient chatClient;

    public MetricQueryAgent(ChatClient.Builder chatClientBuilder,
                           MetricQueryTools metricQueryTools,
                           SkillTools skillTools) {
        this.chatClient = chatClientBuilder
            .defaultSystem("""
                你是【指标查询专家】，专门处理指标数据查询请求。
                
                你的职责：
                1. 理解用户的指标查询需求
                2. 调用工具获取准确数据
                3. 清晰呈现结果
                
                可用指标：招聘数量、专利数量、企业数量、投资金额、人才数量、研发费用
                支持地区：北京、上海、广东、江苏、浙江、全国
                
                你可以使用技能系统了解详细用法。
                
                输出要求：
                - 数据准确，保留2位小数
                - 带单位展示
                - 简要解读数据含义
                """)
            .defaultTools(metricQueryTools, skillTools)
            .build();
    }

    public String handle(String request) {
        return chatClient.prompt()
            .user(request)
            .call()
            .content();
    }
}
