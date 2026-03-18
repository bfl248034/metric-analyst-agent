package com.metric.analyst.agent.agents;

import com.alibaba.cloud.ai.agent.react.ReactAgent;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Component;

/**
 * 澄清智能体
 * 
 * 职责：
 * 1. 当意图不明确时，向用户询问
 * 2. 提供建议的查询方式
 * 3. 引导用户补充必要信息
 */
@Slf4j
@Component
public class ClarificationAgent {

    @Getter
    private final ReactAgent agent;

    public ClarificationAgent(ChatModel chatModel) {
        this.agent = ReactAgent.builder()
            .name("clarification")
            .model(chatModel)
            .instruction("""
                你是澄清专家。当用户查询意图不明确时，礼貌地询问并提供建议。
                
                ## 常见澄清场景
                
                1. 指标不明确
                "您想查询哪个指标？例如：
                - 招聘数量
                - 专利数量  
                - 企业数量"
                
                2. 地区不明确
                "您想查询哪个地区？例如：
                - 北京
                - 上海
                - 全国"
                
                3. 时间不明确
                "您想查询什么时间范围？例如：
                - 最近一个月
                - 2024年1月
                - 最近一年"
                
                4. 查询类型不明确
                "您是想：
                - 查询具体数值
                - 对比不同地区
                - 看趋势变化
                - 了解分布占比"
                
                ## 回答原则
                - 礼貌友好
                - 提供具体示例
                - 不要一次问太多问题
                - 给出最常用的选项
                """)
            .outputKey("clarification_question")
            .build();
        
        log.info("[ClarificationAgent] Initialized");
    }
}
