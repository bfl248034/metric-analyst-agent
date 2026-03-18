package com.metric.analyst.agent.agents;

import com.alibaba.cloud.ai.agent.react.ReactAgent;
import com.alibaba.cloud.ai.memory.MemorySaver;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Component;

/**
 * 指标查询智能体
 * 
 * 职责：
 * 1. 解析指标名称，匹配到具体指标ID
 * 2. 解析维度值（地区、时间、学历等）
 * 3. 构建并执行查询
 * 4. 格式化结果返回
 * 
 * 使用 Tools：
 * - metric_retrieval: 指标检索
 * - dimension_parse: 维度解析
 * - sql_execute: SQL执行
 */
@Slf4j
@Component
public class MetricQueryAgent {

    @Getter
    private final ReactAgent agent;

    public MetricQueryAgent(ChatModel chatModel) {
        this.agent = ReactAgent.builder()
            .name("metric-query")
            .model(chatModel)
            .instruction("""
                你是指标查询专家。根据用户查询，完成以下步骤：
                
                ## 步骤1：指标匹配
                使用 metric_retrieval 工具，根据用户描述找到最匹配的指标。
                
                ## 步骤2：维度解析
                使用 dimension_parse 工具，解析查询中的维度信息：
                - 地区：返回国标编码（6位）
                - 时间：返回YYYYMM格式或recent_N标记
                - 其他维度：返回编码
                
                ## 步骤3：数据查询
                使用 sql_execute 工具，执行查询获取数据。
                
                ## 步骤4：结果格式化
                将查询结果整理为易读的格式，包含：
                - 指标名称和数值
                - 查询条件说明
                - 数据时间范围
                
                ## 输出格式
                ```
                指标：{指标名称}
                数值：{数值} {单位}
                条件：{地区} | {时间} | {其他维度}
                说明：{数据说明}
                ```
                
                注意：
                - 如果指标匹配不明确，询问用户确认
                - 如果维度值无效，使用默认值并说明
                - 地区编码使用6位国标（北京=110000，全国=100000）
                """)
            .saver(new MemorySaver())
            .outputKey("query_result")
            .build();
        
        log.info("[MetricQueryAgent] Initialized");
    }
}
