package com.metric.analyst.agent.controller;

import com.metric.analyst.agent.service.query.DataQueryService;
import com.metric.analyst.agent.service.query.DataQueryService.QueryResult;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

/**
 * 分析服务 API - 简化版
 */
@RestController
@RequestMapping("/api")
public class AnalystController {

    private final DataQueryService queryService;

    public AnalystController(DataQueryService queryService) {
        this.queryService = queryService;
    }

    /**
     * 单指标查询
     * GET /api/query/single?indicator=招聘数量&region=北京&time=最近
     */
    @GetMapping("/query/single")
    public QueryResult querySingle(
            @RequestParam String indicator,
            @RequestParam(defaultValue = "北京") String region,
            @RequestParam(defaultValue = "最近") String time) {
        
        System.out.println("[API] Single query: indicator=" + indicator + ", region=" + region + ", time=" + time);
        return queryService.querySingleMetric(indicator, region, time);
    }

    /**
     * 多地区对比
     * GET /api/query/compare?indicator=招聘数量&regions=北京,上海,广东
     */
    @GetMapping("/query/compare")
    public QueryResult queryCompare(
            @RequestParam String indicator,
            @RequestParam String regions) {
        
        List<String> regionList = Arrays.asList(regions.split(","));
        System.out.println("[API] Compare query: indicator=" + indicator + ", regions=" + regionList);
        return queryService.queryMultiRegion(indicator, regionList, "最近");
    }

    /**
     * 趋势分析
     * GET /api/query/trend?indicator=招聘数量&region=北京&months=6
     */
    @GetMapping("/query/trend")
    public QueryResult queryTrend(
            @RequestParam String indicator,
            @RequestParam(defaultValue = "北京") String region,
            @RequestParam(defaultValue = "6") int months) {
        
        System.out.println("[API] Trend query: indicator=" + indicator + ", region=" + region + ", months=" + months);
        return queryService.queryTrend(indicator, region, months);
    }

    /**
     * 排名查询
     * GET /api/query/ranking?indicator=招聘数量&topN=5
     */
    @GetMapping("/query/ranking")
    public QueryResult queryRanking(
            @RequestParam String indicator,
            @RequestParam(defaultValue = "5") int topN) {
        
        System.out.println("[API] Ranking query: indicator=" + indicator + ", topN=" + topN);
        return queryService.queryRanking(indicator, "最近", topN);
    }

    /**
     * 健康检查
     */
    @GetMapping("/health")
    public String health() {
        return "OK";
    }
}
