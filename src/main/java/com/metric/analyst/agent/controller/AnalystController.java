package com.metric.analyst.agent.controller;

import com.metric.analyst.agent.agent.MetricAnalystAgent;
import com.metric.analyst.agent.tools.MetricQueryTools;
import org.springframework.web.bind.annotation.*;

/**
 * 指标分析 API - 简化版
 */
@RestController
@RequestMapping("/api")
public class AnalystController {

    private final MetricAnalystAgent analystAgent;
    private final MetricQueryTools queryTools;

    public AnalystController(MetricAnalystAgent analystAgent, 
                            MetricQueryTools queryTools) {
        this.analystAgent = analystAgent;
        this.queryTools = queryTools;
    }

    /**
     * AI 对话接口 - 自然语言查询
     * GET /api/chat?input=北京最近招聘数量是多少
     */
    @GetMapping("/chat")
    public String chat(@RequestParam String input) {
        return analystAgent.chat(input);
    }

    /**
     * 直接工具调用接口
     */
    @GetMapping("/query/single")
    public String querySingle(@RequestParam String metric, @RequestParam String region) {
        return queryTools.queryMetricCurrentValue(metric, region);
    }

    @GetMapping("/query/compare")
    public String queryCompare(@RequestParam String metric, @RequestParam String regions) {
        return queryTools.queryMetricComparison(metric, regions);
    }

    @GetMapping("/query/trend")
    public String queryTrend(@RequestParam String metric, 
                            @RequestParam String region,
                            @RequestParam(defaultValue = "6") int months) {
        return queryTools.queryMetricTrend(metric, region, months);
    }

    @GetMapping("/query/ranking")
    public String queryRanking(@RequestParam String metric,
                              @RequestParam(defaultValue = "5") int topN) {
        return queryTools.queryMetricRanking(metric, topN);
    }

    @GetMapping("/health")
    public String health() {
        return "OK - Metric Analyst Agent is running";
    }
}
