package com.metric.analyst.agent.tools;

import com.metric.analyst.agent.service.DataQueryService;
import com.metric.analyst.agent.service.DimensionNormalizationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 指标查询工具集
 * 
 * 共享工具，可被多个 Agent 调用
 * 使用 Spring AI @Tool 注解定义
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MetricQueryTools {

    private final DataQueryService dataQueryService;
    private final DimensionNormalizationService dimensionService;

    /**
     * 标准化维度值
     */
    @Tool(description = "将用户输入的维度值（地区名称、时间表达式等）标准化为系统编码")
    public NormalizedDimensions normalizeDimensions(
        @ToolParam(description = "指标编码，如 I_RPA_ICN_RAE_SALARY_AMOUNT") String indicatorId,
        @ToolParam(description = "维度映射，如 {region: \"北京\", time: \"latest\"}") Map<String, Object> dimensions
    ) {
        log.info("Normalizing dimensions for indicator: {}", indicatorId);
        return dimensionService.normalize(indicatorId, dimensions);
    }

    /**
     * 查询指标当前值
     */
    @Tool(description = "查询单个指标在指定地区的当前值")
    public QueryResult queryCurrentValue(
        @ToolParam(description = "指标编码") String indicatorId,
        @ToolParam(description = "标准化后的维度") NormalizedDimensions dimensions
    ) {
        log.info("Querying current value for indicator: {}", indicatorId);
        return dataQueryService.queryCurrent(indicatorId, dimensions);
    }

    /**
     * 查询指标趋势
     */
    @Tool(description = "查询指标的历史趋势")
    public TrendResult queryTrend(
        @ToolParam(description = "指标编码") String indicatorId,
        @ToolParam(description = "地区编码，如 110000") String regionCode,
        @ToolParam(description = "查询月数，1-24") int months
    ) {
        log.info("Querying trend for indicator: {}, months: {}", indicatorId, months);
        return dataQueryService.queryTrend(indicatorId, regionCode, months);
    }

    /**
     * 查询指标排名
     */
    @Tool(description = "查询指标的地区排名")
    public RankingResult queryRanking(
        @ToolParam(description = "指标编码") String indicatorId,
        @ToolParam(description = "地区级别：省级/市级/全国") String regionLevel,
        @ToolParam(description = "返回数量，默认10，最大20") int topN
    ) {
        log.info("Querying ranking for indicator: {}, level: {}", indicatorId, regionLevel);
        return dataQueryService.queryRanking(indicatorId, regionLevel, topN);
    }

    /**
     * 对比多地区指标
     */
    @Tool(description = "对比多个地区的同一指标")
    public ComparisonResult queryComparison(
        @ToolParam(description = "指标编码") String indicatorId,
        @ToolParam(description = "地区编码列表") List<String> regionCodes,
        @ToolParam(description = "时间") String time
    ) {
        log.info("Querying comparison for indicator: {}, regions: {}", indicatorId, regionCodes);
        return dataQueryService.queryComparison(indicatorId, regionCodes, time);
    }

    // ========== DTO 类 ==========
    
    public record NormalizedDimensions(
        String indicatorId,
        Map<String, DimensionValue> dimensions
    ) {}

    public record DimensionValue(
        String code,
        String name
    ) {}

    public record QueryResult(
        String indicatorId,
        String indicatorName,
        BigDecimal value,
        String unit,
        String timeId,
        BigDecimal yoy,
        BigDecimal mom,
        boolean success,
        String message
    ) {}

    public record TrendResult(
        String indicatorId,
        List<TimePoint> dataPoints,
        String trendType,  // 上升/下降/平稳
        boolean success
    ) {
        public record TimePoint(
            String timeId,
            BigDecimal value,
            BigDecimal yoy,
            BigDecimal mom
        ) {}
    }

    public record RankingResult(
        String indicatorId,
        List<RankItem> rankings,
        boolean success
    ) {
        public record RankItem(
            int rank,
            String regionId,
            String regionName,
            BigDecimal value
        ) {}
    }

    public record ComparisonResult(
        String indicatorId,
        List<RegionData> regions,
        boolean success
    ) {
        public record RegionData(
            String regionId,
            String regionName,
            BigDecimal value,
            int rank
        ) {}
    }
}
