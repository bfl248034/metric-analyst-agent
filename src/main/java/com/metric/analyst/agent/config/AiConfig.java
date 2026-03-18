package com.metric.analyst.agent.config;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.ai.chat.model.ChatModel;

/**
 * AI 模型配置
 */
@Configuration
public class AiConfig {

    @Value("${dashscope.api-key:${AI_DASHSCOPE_API_KEY:}}")
    private String apiKey;

    @Value("${dashscope.model:qwen-turbo}")
    private String model;

    @Bean
    public ChatModel chatModel() {
        DashScopeApi dashScopeApi = DashScopeApi.builder()
            .apiKey(apiKey)
            .build();

        return DashScopeChatModel.builder()
            .dashScopeApi(dashScopeApi)
            .defaultOptions(DashScopeChatOptions.builder()
                .withModel(model)
                .withTemperature(0.3)
                .withMaxTokens(2000)
                .build())
            .build();
    }
}
