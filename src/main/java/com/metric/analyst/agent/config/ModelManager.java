package com.metric.analyst.agent.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * 多模型管理器 - 支持 DashScope(千问) 和 OpenAI 兼容模型
 */
@Configuration
public class ModelManager {

    @Value("${model.default-provider:dashscope}")
    private String defaultProvider;

    @Autowired
    @Qualifier("dashscopeChatClient")
    private ChatClient dashscopeChatClient;

    @Autowired
    @Qualifier("openaiChatClient")
    private ChatClient openaiChatClient;

    /**
     * 获取默认的 ChatClient
     */
    public ChatClient getDefaultClient() {
        return getClient(defaultProvider);
    }

    /**
     * 根据 provider 获取 ChatClient
     * @param provider dashscope | openai
     */
    public ChatClient getClient(String provider) {
        return switch (provider.toLowerCase()) {
            case "dashscope", "qwen" -> dashscopeChatClient;
            case "openai", "kimi", "deepseek" -> openaiChatClient;
            default -> dashscopeChatClient;
        };
    }

    /**
     * 获取所有可用的模型提供商
     */
    public String[] getAvailableProviders() {
        return new String[]{"dashscope", "openai"};
    }

    /**
     * 获取当前默认提供商
     */
    public String getDefaultProvider() {
        return defaultProvider;
    }
}
