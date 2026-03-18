package com.metric.analyst.agent.agents;

import com.alibaba.cloud.ai.agent.Agent;
import com.alibaba.cloud.ai.agent.react.ReactAgent;
import com.alibaba.cloud.ai.agent.supervisor.SupervisorAgent;
import com.alibaba.cloud.ai.common.RunnableConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.Map;

/**
 * 指标分析主管智能体
 * 
 * 职责：
 * 1. 接收用户查询
 * 2. 识别查询类型（单指标查询/多指标分析/闲聊/澄清）
 * 3. 路由到对应的子智能体
 * 4. 支持并行执行多个子智能体
 * 5. 汇总结果返回给用户
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AnalystSupervisor {

    private final ChatModel chatModel;
    
    // 子智能体
    private final IntentRecognitionAgent intentAgent;
    private final MetricQueryAgent metricQueryAgent;
    private final MultiMetricAnalysisAgent multiMetricAnalysisAgent;
    private final ChitchatAgent chitchatAgent;
    private final ClarificationAgent clarificationAgent;

    private SupervisorAgent supervisorAgent;

    @PostConstruct
    public void init() {
        // 构建主管智能体
        this.supervisorAgent = SupervisorAgent.builder()
            .name("analyst-supervisor")
            .model(chatModel)
            .instruction("""
                你是指标分析系统的主管智能体。你的职责是：
                
                1. 分析用户查询的意图类型
                2. 将任务分配给最合适的子智能体处理
                3. 支持并行调用多个子智能体
                4. 整合各子智能体的结果，给出最终回答
                
                可用子智能体：
                - intent-recognition: 意图识别，分析查询类型
                - metric-query: 单指标查询
                - multi-metric-analysis: 多指标对比分析
                - chitchat: 闲聊问答
                - clarification: 需要用户澄清时
                
                路由规则：
                - 单指标查询（如"北京上个月招聘数"）→ metric-query
                - 多指标对比（如"北京和上海招聘数对比"）→ multi-metric-analysis
                - 闲聊/问候 → chitchat
                - 意图不明确 → clarification
                
                输出格式：分析过程 + 最终结果
                """)
            .subAgents(List.of(
                intentAgent.getAgent(),
                metricQueryAgent.getAgent(),
                multiMetricAnalysisAgent.getAgent(),
                chitchatAgent.getAgent(),
                clarificationAgent.getAgent()
            ))
            .build();
        
        log.info("[AnalystSupervisor] SupervisorAgent initialized with {} sub-agents", 
            supervisorAgent.getSubAgents().size());
    }

    /**
     * 处理用户查询
     */
    public AnalystResponse processQuery(String query, String sessionId) {
        log.info("[AnalystSupervisor] Processing query: {}", query);
        
        RunnableConfig config = RunnableConfig.builder()
            .threadId(sessionId)
            .build();
        
        try {
            // 构建输入，包含会话上下文
            Map<String, Object> input = Map.of(
                "query", query,
                "sessionId", sessionId,
                "timestamp", System.currentTimeMillis()
            );
            
            // 调用主管智能体
            var result = supervisorAgent.call(input, config);
            
            return AnalystResponse.builder()
                .success(true)
                .content(result.getOutput().getText())
                .sessionId(sessionId)
                .build();
                
        } catch (Exception e) {
            log.error("[AnalystSupervisor] Failed to process query", e);
            return AnalystResponse.builder()
                .success(false)
                .errorMessage("处理失败: " + e.getMessage())
                .sessionId(sessionId)
                .build();
        }
    }

    /**
     * 流式处理
     */
    public void processQueryStream(String query, String sessionId, 
                                   java.util.function.Consumer<String> onChunk) {
        RunnableConfig config = RunnableConfig.builder()
            .threadId(sessionId)
            .build();
        
        Map<String, Object> input = Map.of("query", query);
        
        supervisorAgent.stream(input, config)
            .subscribe(
                chunk -> onChunk.accept(chunk.getOutput().getText()),
                error -> log.error("Stream error", error)
            );
    }

    /**
     * 响应对象
     */
    @lombok.Data
    @lombok.Builder
    public static class AnalystResponse {
        private boolean success;
        private String content;
        private String errorMessage;
        private String sessionId;
    }
}
