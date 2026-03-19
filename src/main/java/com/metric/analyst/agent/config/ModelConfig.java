package com.metric.analyst.agent.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * 模型配置类 - 配置多个 ChatClient
 */
@Configuration
public class ModelConfig {

    @Value("${spring.ai.openai.api-key:demo-key}")
    private String openaiApiKey;

    @Value("${spring.ai.openai.base-url:https://api.openai.com}")
    private String openaiBaseUrl;

    @Value("${spring.ai.openai.chat.options.model:gpt-4o}")
    private String openaiModel;

    /**
     * DashScope (通义千问) ChatClient
     * 使用 @Qualifier 指定使用 DashScope 的 ChatModel
     */
    @Bean
    @Primary
    public ChatClient dashscopeChatClient(@Qualifier("dashScopeChatModel") ChatModel dashscopeModel) {
        return ChatClient.builder(dashscopeModel).build();
    }

    /**
     * OpenAI 兼容接口 ChatClient
     * 手动配置 OpenAiChatModel
     */
    @Bean
    public ChatClient openaiChatClient() {
        OpenAiApi openAiApi = OpenAiApi.builder()
                .apiKey(openaiApiKey)
                .baseUrl(openaiBaseUrl)
                .build();
        
        OpenAiChatModel openAiChatModel = OpenAiChatModel.builder()
                .openAiApi(openAiApi)
                .defaultOptions(OpenAiChatOptions.builder()
                        .model(openaiModel)
                        .temperature(0.7)
                        .build())
                .build();
        
        return ChatClient.builder(openAiChatModel).build();
    }
}
