package com.metric.analyst.agent.controller;

import com.metric.analyst.agent.agents.MetricAnalystOrchestrator;
import com.metric.analyst.agent.agents.MetricQueryTools;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 指标分析 API - 基于 Spring AI Alibaba Agent Framework
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AnalystController {

    private final MetricAnalystOrchestrator orchestrator;
    private final MetricQueryTools queryTools;

    /**
     * AI 对话接口 - 多智能体协调处理
     * GET /api/chat?input=北京招聘数量是多少
     * GET /api/chat?input=北京招聘数量是多少&threadId=user_123
     */
    @GetMapping("/chat")
    public String chat(@RequestParam String input,
                       @RequestParam(required = false) String threadId) {
        if (threadId != null) {
            return orchestrator.handle(input, threadId);
        }
        return orchestrator.handle(input);
    }

    /**
     * 直接调用指标查询专家
     * GET /api/query?input=北京招聘数量
     */
    @GetMapping("/query")
    public String query(@RequestParam String input) {
        return orchestrator.queryMetric(input);
    }

    /**
     * 直接调用洞察分析专家
     * GET /api/analyze?input=分析一下趋势
     */
    @GetMapping("/analyze")
    public String analyze(@RequestParam String input) {
        return orchestrator.analyzeInsight(input);
    }

    /**
     * 直接调用报告生成专家
     * GET /api/report?input=生成报告
     */
    @GetMapping("/report")
    public String report(@RequestParam String input) {
        return orchestrator.generateReport(input);
    }

    /**
     * 获取可用的 Agent 列表
     * GET /api/agents
     */
    @GetMapping("/agents")
    public Map<String, Object> getAgents() {
        Map<String, Object> result = new HashMap<>();
        result.put("orchestrator", "metric_analyst_supervisor");
        result.put("agents", new String[]{
                "metric_query_expert",
                "insight_analyst",
                "report_generator",
                "dimension_parser"
        });
        return result;
    }

    // ============ 直接工具调用接口（保留用于精确控制）============

    /**
     * 查询单个指标当前值
     * GET /api/tools/single?metric=招聘数量&region=北京
     */
    @GetMapping("/tools/single")
    public String querySingle(@RequestParam String metric, @RequestParam String region) {
        return queryTools.queryMetricCurrentValue(metric, region);
    }

    /**
     * 多地区指标对比
     * GET /api/tools/compare?metric=招聘数量&regions=北京,上海,杭州
     */
    @GetMapping("/tools/compare")
    public String queryCompare(@RequestParam String metric, @RequestParam String regions) {
        return queryTools.queryMetricComparison(metric, regions);
    }

    /**
     * 查询指标趋势
     * GET /api/tools/trend?metric=招聘数量&region=北京&months=6
     */
    @GetMapping("/tools/trend")
    public String queryTrend(@RequestParam String metric,
                             @RequestParam String region,
                             @RequestParam(defaultValue = "6") int months) {
        return queryTools.queryMetricTrend(metric, region, months);
    }

    /**
     * 查询指标排名
     * GET /api/tools/ranking?metric=招聘数量&topN=5
     */
    @GetMapping("/tools/ranking")
    public String queryRanking(@RequestParam String metric,
                               @RequestParam(defaultValue = "5") int topN) {
        return queryTools.queryMetricRanking(metric, topN);
    }

    /**
     * 提取维度信息
     * GET /api/tools/extract?input=北京招聘数量多少
     */
    @GetMapping("/tools/extract")
    public String extractDimensions(@RequestParam String input) {
        return queryTools.extractDimensions(input);
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
