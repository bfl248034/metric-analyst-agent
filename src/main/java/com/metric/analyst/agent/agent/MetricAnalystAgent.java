package com.metric.analyst.agent.agent;

import com.metric.analyst.agent.tools.MetricQueryTools;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

/**
 * 指标分析 Agent - 基于 ChatClient + Tools 模式
 */
@Component
public class MetricAnalystAgent {

    private final ChatClient chatClient;

    public MetricAnalystAgent(ChatClient.Builder chatClientBuilder, 
                              MetricQueryTools metricQueryTools) {
        this.chatClient = chatClientBuilder
            .defaultSystem("""
                你是指标分析助手，专门帮助用户查询和分析各类业务指标数据。
                
                可用指标包括：
                - 招聘数量：反映用工需求
                - 专利数量：反映创新产出
                - 企业数量：反映市场主体
                - 投资金额：反映投资热度
                - 人才数量：反映人才储备
                - 研发费用：反映创新投入
                
                支持地区：北京、上海、广东、江苏、浙江、全国
                
                你可以：
                1. 查询单指标当前值
                2. 对比多地区数据
                3. 查看历史趋势
                4. 查看地区排名
                
                回答要简洁专业，数据要准确。
                """)
            .defaultTools(metricQueryTools)
            .build();
    }

    /**
     * 处理用户查询
     */
    public String chat(String userInput) {
        return chatClient.prompt()
            .user(userInput)
            .call()
            .content();
    }
}
