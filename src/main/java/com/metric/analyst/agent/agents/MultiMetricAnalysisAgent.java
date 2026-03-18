package com.metric.analyst.agent.agents;

import com.alibaba.cloud.ai.agent.parallel.ParallelAgent;
import com.alibaba.cloud.ai.agent.react.ReactAgent;
import com.alibaba.cloud.ai.memory.MemorySaver;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 多指标分析智能体
 * 
 * 职责：
 * 1. 识别多个相关指标
 * 2. 并行查询各指标数据
 * 3. 进行对比分析（同比、环比、占比等）
 * 4. 生成综合分析报告
 * 
 * 实现方式：使用 ParallelAgent 并行执行多个 MetricQueryAgent 实例
 */
@Slf4j
@Component
public class MultiMetricAnalysisAgent {

    @Getter
    private final ReactAgent agent;
    private final ChatModel chatModel;

    public MultiMetricAnalysisAgent(ChatModel chatModel) {
        this.chatModel = chatModel;
        
        this.agent = ReactAgent.builder()
            .name("multi-metric-analysis")
            .model(chatModel)
            .instruction("""
                你是多指标分析专家。处理涉及多个指标的对比分析任务：
                
                ## 分析流程
                1. 识别用户查询涉及的所有指标
                2. 使用 parallel-metric-query 并行查询各指标数据
                3. 对比分析（差值、比值、趋势）
                4. 生成综合分析报告
                
                ## 分析类型
                - 横向对比：不同地区同一指标对比
                - 纵向对比：同一地区不同时间对比
                - 多指标关联：不同指标间的关系分析
                
                ## 输出格式
                ```
                ## 多指标分析报告
                
                ### 查询指标
                - 指标1：{名称}
                - 指标2：{名称}
                
                ### 数据对比
                | 维度 | 指标1 | 指标2 | 差值 | 比值 |
                |------|-------|-------|------|------|
                | ... | ... | ... | ... | ... |
                
                ### 分析结论
                {综合分析结论}
                
                ### 建议
                {基于数据的建议}
                ```
                """)
            .saver(new MemorySaver())
            .outputKey("analysis_result")
            .build();
        
        log.info("[MultiMetricAnalysisAgent] Initialized");
    }

    /**
     * 创建并行查询Agent（用于内部调用）
     */
    public ParallelAgent createParallelQueryAgent(List<String> indicatorIds) {
        // 为每个指标创建一个查询Agent
        List<ReactAgent> queryAgents = indicatorIds.stream()
            .map(indicatorId -> ReactAgent.builder()
                .name("query-" + indicatorId)
                .model(chatModel)
                .instruction("查询指标: " + indicatorId)
                .outputKey("result_" + indicatorId)
                .build())
            .toList();
        
        return ParallelAgent.builder()
            .name("parallel-metric-query")
            .subAgents(queryAgents)
            .mergeOutputKey("parallel_results")
            .build();
    }
}
