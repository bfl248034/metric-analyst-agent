package com.metric.analyst.agent.controller;

import com.metric.analyst.agent.agents.MetricAnalystAgent;
import com.metric.analyst.agent.agents.MetricAnalystAgent.AgentResponse;
import com.metric.analyst.agent.agents.MetricAnalystAgent.ConversationContext;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 指标分析 API - 基于 Spring AI Alibaba Agent Framework
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AnalystController {

    private final MetricAnalystAgent agent;

    /**
     * AI 对话接口 - 智能体处理
     * GET /api/chat?input=北京招聘数量是多少
     * GET /api/chat?input=北京招聘数量是多少&sessionId=user_123
     */
    @GetMapping("/chat")
    public Map<String, Object> chat(@RequestParam String input,
                                    @RequestParam(required = false) String sessionId) {
        if (sessionId == null) {
            sessionId = UUID.randomUUID().toString();
        }

        ConversationContext context = ConversationContext.builder()
            .sessionId(sessionId)
            .attributes(new HashMap<>())
            .build();

        AgentResponse response = agent.handle(input, context);

        Map<String, Object> result = new HashMap<>();
        result.put("sessionId", sessionId);
        result.put("type", response.getType().name());
        result.put("content", response.getContent());
        if (response.getData() != null) {
            result.put("data", response.getData());
        }
        return result;
    }

    /**
     * 获取可用的指标领域
     * GET /api/domains
     */
    @GetMapping("/domains")
    public Map<String, Object> getDomains() {
        Map<String, Object> result = new HashMap<>();
        result.put("domains", new String[]{
            "招聘就业",
            "市场主体",
            "知识产权",
            "政府采购"
        });
        result.put("examples", new String[]{
            "北京招聘薪资",
            "各省份企业新增排名",
            "近6个月专利趋势",
            "政府采购金额排名"
        });
        return result;
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        Map<String, String> result = new HashMap<>();
        result.put("status", "UP");
        result.put("version", "3.0.0");
        result.put("framework", "Spring AI Alibaba Agent Framework");
        return result;
    }
}
