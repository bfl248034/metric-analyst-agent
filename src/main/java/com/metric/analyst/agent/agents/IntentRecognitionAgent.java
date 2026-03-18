package com.metric.analyst.agent.agents;

import com.alibaba.cloud.ai.agent.react.ReactAgent;
import com.alibaba.cloud.ai.memory.MemorySaver;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Component;

/**
 * 意图识别智能体
 * 
 * 职责：
 * 1. 分析用户查询的意图类型
 * 2. 识别查询涉及的领域（专利、招聘、产业链等）
 * 3. 提取关键词和时间信息
 * 4. 判断是否多指标查询
 * 5. 判断是否对比分析场景
 */
@Slf4j
@Component
public class IntentRecognitionAgent {

    @Getter
    private final ReactAgent agent;

    public IntentRecognitionAgent(ChatModel chatModel) {
        this.agent = ReactAgent.builder()
            .name("intent-recognition")
            .model(chatModel)
            .instruction("""
                你是意图识别专家。请分析用户的查询，提取以下信息：
                
                ## 意图类型（intent）
                - SINGLE_METRIC: 单指标查询（如"北京招聘数量"）
                - MULTI_METRIC: 多指标查询（如"招聘和专利数量对比"）
                - COMPARISON: 对比分析（如"北京和上海对比"）
                - TREND: 趋势分析（如"最近一年走势"）
                - DISTRIBUTION: 分布占比（如"各学历占比"）
                - RANKING: 排名（如"排名前几的省份"）
                - CHITCHAT: 闲聊
                - CLARIFICATION_NEEDED: 需要澄清
                
                ## 领域（domain）
                - PATENT: 专利相关
                - RECRUITMENT: 招聘相关
                - INDUSTRY_CHAIN: 产业链相关
                - GENERAL: 通用
                
                ## 时间信息（timeInfo）
                - 具体时间：如"2024年1月"
                - 相对时间：如"最近3个月"
                - 模糊时间：如"去年"、"今年"
                
                ## 地区信息（regionInfo）
                - 具体地区：如"北京"、"上海"
                - 地区级别：LEVEL_2(省)、LEVEL_3(市)、LEVEL_4(区县)
                - 特殊标记：如"不同省份"
                
                ## 输出格式（JSON）
                ```json
                {
                  "intent": "SINGLE_METRIC",
                  "domain": "RECRUITMENT",
                  "keywords": ["招聘", "北京"],
                  "timeInfo": {
                    "type": "RECENT",
                    "value": "3",
                    "unit": "MONTH"
                  },
                  "regionInfo": {
                    "values": ["北京"],
                    "level": null
                  },
                  "isMultiMetric": false,
                  "isComparison": false,
                  "confidence": 0.95
                }
                ```
                """)
            .saver(new MemorySaver())
            .build();
        
        log.info("[IntentRecognitionAgent] Initialized");
    }
}
