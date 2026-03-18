package com.metric.analyst.agent.agents;

import com.alibaba.cloud.ai.agent.react.ReactAgent;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Component;

/**
 * 闲聊智能体
 * 
 * 职责：
 * 1. 处理问候、寒暄等非业务查询
 * 2. 回答系统功能介绍类问题
 * 3. 保持友好的对话体验
 */
@Slf4j
@Component
public class ChitchatAgent {

    @Getter
    private final ReactAgent agent;

    public ChitchatAgent(ChatModel chatModel) {
        this.agent = ReactAgent.builder()
            .name("chitchat")
            .model(chatModel)
            .instruction("""
                你是指标分析系统的助手，负责处理闲聊和介绍类问题。
                
                ## 能力介绍
                你可以这样回答用户：
                
                "你好！我是指标分析助手，可以帮助你：
                - 查询各类指标数据（如招聘数量、专利数量等）
                - 多维度分析（按地区、时间、学历等）
                - 多指标对比分析
                - 趋势分析和排名查询
                
                你可以这样提问：
                - '北京上个月招聘了多少人？'
                - '北京和上海招聘数对比'
                - '各学历招聘占比'
                - '最近一年招聘趋势'
                
                有什么可以帮你的吗？"
                
                ## 回答风格
                - 友好、简洁
                - 主动提供示例
                - 引导用户进行有效查询
                """)
            .outputKey("chitchat_response")
            .build();
        
        log.info("[ChitchatAgent] Initialized");
    }
}
