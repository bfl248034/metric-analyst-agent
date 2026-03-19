package com.metric.analyst.agent.agents;

import java.util.List;
import java.util.Optional;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Component;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.agent.flow.agent.SupervisorAgent;

/**
 * 指标分析主管 Agent - 基于 Spring AI Alibaba SupervisorAgent
 * 协调多个子智能体完成复杂任务
 */
@Component
public class MetricAnalystOrchestrator {

    private final SupervisorAgent supervisorAgent;
    private final ReactAgent metricQueryAgent;
    private final ReactAgent insightAgent;
    private final ReactAgent reportAgent;

    public MetricAnalystOrchestrator(
            ChatModel chatModel,
            ReactAgent metricQueryAgent,
            ReactAgent insightAgent,
            ReactAgent reportAgent,
            ReactAgent dimensionParserAgent) {

        this.metricQueryAgent = metricQueryAgent;
        this.insightAgent = insightAgent;
        this.reportAgent = reportAgent;

        // 创建 SupervisorAgent 来协调子 Agent
        this.supervisorAgent = SupervisorAgent.builder()
                .name("metric_analyst_supervisor")
                .description("指标分析主管，协调查询、分析和报告生成任务")
                .model(chatModel)
                .systemPrompt("""
                    你是【指标分析主管】，负责协调多个专业Agent完成用户的数据分析需求。

                    你的团队：
                    1. 【metric_query_expert】- 指标查询专家
                       - 处理具体的数据查询：单指标查询、多地区对比、趋势分析、排名查询
                       - 适用于："北京招聘数量多少"、"对比上海和深圳的薪资"等

                    2. 【insight_analyst】- 洞察分析专家
                       - 处理深度分析：异常检测、关联分析、预测建议
                       - 适用于："分析一下趋势"、"为什么数据下降了"等

                    3. 【report_generator】- 报告生成专家
                       - 生成综合分析报告
                       - 适用于："生成报告"、"总结一下"等

                    决策规则：
                    - 简单查询（具体数值、排名、对比）→ metric_query_expert
                    - 深度分析（异常、关联、原因分析）→ insight_analyst
                    - 生成报告或总结 → report_generator
                    - 多步骤任务（如"查询然后分析"）→ 先查询后分析

                    响应格式：
                    只返回Agent名称（metric_query_expert、insight_analyst、report_generator）或 FINISH，不要包含其他解释。
                    """)
                .subAgents(List.of(metricQueryAgent, insightAgent, reportAgent))
                .build();
    }

    /**
     * 处理用户请求
     */
    public String handle(String userInput) {
        try {
            // 使用 SupervisorAgent 协调处理
            Optional<OverAllState> result = supervisorAgent.invoke(userInput);

            if (result.isPresent()) {
                OverAllState state = result.get();

                // 获取最终消息
                Optional<Object> messagesOpt = state.value("messages");
                if (messagesOpt.isPresent()) {
                    List<? extends org.springframework.ai.chat.messages.Message> messages =
                            (List<? extends org.springframework.ai.chat.messages.Message>) messagesOpt.get();

                    // 获取最后一条 AssistantMessage
                    for (int i = messages.size() - 1; i >= 0; i--) {
                        org.springframework.ai.chat.messages.Message msg = messages.get(i);
                        if (msg instanceof AssistantMessage assistantMsg) {
                            return assistantMsg.getText();
                        }
                    }
                }

                return "处理完成，但未获取到结果";
            }

            return "处理失败，请稍后重试";

        } catch (Exception e) {
            return "处理出错: " + e.getMessage();
        }
    }

    /**
     * 处理用户请求（带线程ID，支持多轮对话）
     */
    public String handle(String userInput, String threadId) {
        try {
            RunnableConfig config = RunnableConfig.builder()
                    .threadId(threadId)
                    .build();

            // 使用 invoke 方法并传入 config
            Optional<OverAllState> result = supervisorAgent.invoke(userInput, config);

            if (result.isPresent()) {
                OverAllState state = result.get();

                Optional<Object> messagesOpt = state.value("messages");
                if (messagesOpt.isPresent()) {
                    List<? extends org.springframework.ai.chat.messages.Message> messages =
                            (List<? extends org.springframework.ai.chat.messages.Message>) messagesOpt.get();

                    for (int i = messages.size() - 1; i >= 0; i--) {
                        org.springframework.ai.chat.messages.Message msg = messages.get(i);
                        if (msg instanceof AssistantMessage assistantMsg) {
                            return assistantMsg.getText();
                        }
                    }
                }
            }

            return "处理失败，请稍后重试";

        } catch (Exception e) {
            return "处理出错: " + e.getMessage();
        }
    }

    /**
     * 直接调用指标查询专家
     */
    public String queryMetric(String userInput) {
        try {
            AssistantMessage response = metricQueryAgent.call(userInput);
            return response.getText();
        } catch (Exception e) {
            return "查询出错: " + e.getMessage();
        }
    }

    /**
     * 直接调用洞察分析专家
     */
    public String analyzeInsight(String userInput) {
        try {
            AssistantMessage response = insightAgent.call(userInput);
            return response.getText();
        } catch (Exception e) {
            return "分析出错: " + e.getMessage();
        }
    }

    /**
     * 直接调用报告生成专家
     */
    public String generateReport(String userInput) {
        try {
            AssistantMessage response = reportAgent.call(userInput);
            return response.getText();
        } catch (Exception e) {
            return "生成报告出错: " + e.getMessage();
        }
    }
}
