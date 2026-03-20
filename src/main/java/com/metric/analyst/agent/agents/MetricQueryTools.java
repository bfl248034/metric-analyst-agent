package com.metric.analyst.agent.agents;

import com.metric.analyst.agent.dto.MetricComparisonDTO;
import com.metric.analyst.agent.entity.Indicator;
import com.metric.analyst.agent.service.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 指标查询工具类 - 使用 @Tool 注解定义工具
 * 基于 Spring AI Alibaba 官方最佳实践
 * 
 * 解耦设计：依赖 IndicatorLocator 接口而非具体的 IndicatorRecognitionService
 * 避免循环依赖：IndicatorRecognitionService -> ChatModel -> Tools -> ...
 */
@Slf4j
@Component
public class MetricQueryTools {

    private final IndicatorLocator indicatorLocator;
    private final DimensionNormalizationService dimensionService;
    private final DataQueryService dataQueryService;

    public MetricQueryTools(IndicatorLocator indicatorLocator,
                            DimensionNormalizationService dimensionService,
                            DataQueryService dataQueryService) {
        this.indicatorLocator = indicatorLocator;
        this.dimensionService = dimensionService;
        this.dataQueryService = dataQueryService;
    }

    // 地区名称映射
    private static final Map<String, String> REGION_NAME_MAP = Map.ofEntries(
            Map.entry("北京", "110000"),
            Map.entry("上海", "310000"),
            Map.entry("广州", "440100"),
            Map.entry("深圳", "440300"),
            Map.entry("杭州", "330100"),
            Map.entry("苏州", "320500"),
            Map.entry("武汉", "420100"),
            Map.entry("成都", "510100"),
            Map.entry("重庆", "500000"),
            Map.entry("天津", "120000"),
            Map.entry("西安", "610100")
    );

    /**
     * 查询单个指标在指定地区的当前值
     */
    @Tool(description = "查询单个指标在指定地区的当前值。参数：metricName(指标名称如'招聘数量')，regionName(地区名称如'北京')")
    public String queryMetricCurrentValue(
            @ToolParam(description = "指标名称，如：招聘数量、平均薪资") String metricName,
            @ToolParam(description = "地区名称，如：北京、上海、杭州") String regionName) {

        log.info("查询指标当前值: metric={}, region={}", metricName, regionName);

        // 使用 locator 找到指标（解耦，不依赖 RecognitionService）
        IndicatorLocator.RecognitionResult recognition = 
            indicatorLocator.recognize(regionName + metricName);
        
        if (!recognition.isMatched()) {
            return "未找到指标: " + metricName + "。可用指标：招聘数量、平均薪资";
        }

        Indicator indicator = recognition.getIndicator();

        // 标准化维度
        Map<String, Object> dims = new HashMap<>();
        dims.put("region", regionName);
        dims.put("time", "latest");
        
        DimensionNormalizationService.NormalizedDimensions normalized = 
            dimensionService.normalize(
                indicator.getIndicatorId(),
                indicator.getTableId(),
                dims
            );

        // 查询数据（使用新的动态数据源架构）
        DataQueryService.QueryResult result = dataQueryService.query(
            indicator.getTableId(),
            normalized
        );

        if (!result.isSuccess() || result.getRows() == null || result.getRows().isEmpty()) {
            return String.format("未找到 %s 在 %s 的数据", metricName, regionName);
        }

        // 从 DataRow 结构获取数据
        DataQueryService.DataRow latestRow = result.getRows().get(0);

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%s的%s数据：\n", regionName, metricName));
        sb.append(String.format("- 数值：%s %s\n",
                latestRow.getValue() != null ? latestRow.getValue().toPlainString() : "N/A",
                getUnit(metricName)));

        return sb.toString();
    }

    /**
     * 对比多个地区的同一指标
     */
    @Tool(description = "对比多个地区的同一指标。参数：metricName(指标名称)，regionNames(地区名称列表，用逗号分隔)")
    public String queryMetricComparison(
            @ToolParam(description = "指标名称，如：招聘数量、平均薪资") String metricName,
            @ToolParam(description = "地区名称列表，用逗号分隔，如：北京,上海,杭州") String regionNames) {

        log.info("对比指标: metric={}, regions={}", metricName, regionNames);

        // 使用 locator 找到指标
        IndicatorLocator.RecognitionResult recognition = 
            indicatorLocator.recognize(metricName);
        
        if (!recognition.isMatched()) {
            return "未找到指标: " + metricName;
        }

        Indicator indicator = recognition.getIndicator();
        String[] regions = regionNames.split("[，,]");
        List<MetricComparisonDTO.RegionData> dataList = new ArrayList<>();

        for (String region : regions) {
            String regionName = region.trim();
            
            Map<String, Object> dims = new HashMap<>();
            dims.put("region", regionName);
            dims.put("time", "latest");
            
            DimensionNormalizationService.NormalizedDimensions normalized = 
                dimensionService.normalize(
                    indicator.getIndicatorId(),
                    indicator.getTableId(),
                    dims
                );

            DataQueryService.QueryResult result = dataQueryService.query(
                indicator.getTableId(),
                normalized
            );

            if (result.isSuccess() && result.getRows() != null && !result.getRows().isEmpty()) {
                DataQueryService.DataRow row = result.getRows().get(0);
                dataList.add(new MetricComparisonDTO.RegionData(
                        regionName, row.getValue(), null));
            }
        }

        if (dataList.isEmpty()) {
            return "未找到任何地区的数据";
        }

        // 按数值排序
        dataList.sort((a, b) -> b.getValue().compareTo(a.getValue()));

        StringBuilder result = new StringBuilder();
        result.append(String.format("%s地区对比（最新数据）：\n\n", metricName));

        BigDecimal maxValue = dataList.get(0).getValue();
        for (int i = 0; i < dataList.size(); i++) {
            MetricComparisonDTO.RegionData data = dataList.get(i);
            String bar = generateBar(data.getValue(), maxValue, 20);
            result.append(String.format("%d. %s: %s %s %s\n",
                    i + 1,
                    data.getRegionName(),
                    data.getValue() != null ? data.getValue().toPlainString() : "N/A",
                    getUnit(metricName),
                    bar));
            if (data.getYoy() != null) {
                result.append(String.format("   同比: %+.1f%%\n", data.getYoy()));
            }
        }

        return result.toString();
    }

    /**
     * 查询指标的历史趋势
     */
    @Tool(description = "查询指标的历史趋势。参数：metricName(指标名称)，regionName(地区名称)，months(月数，默认6个月)")
    public String queryMetricTrend(
            @ToolParam(description = "指标名称，如：招聘数量、平均薪资") String metricName,
            @ToolParam(description = "地区名称，如：北京、上海") String regionName,
            @ToolParam(description = "查询最近几个月的数据，默认6个月") int months) {

        log.info("查询趋势: metric={}, region={}, months={}", metricName, regionName, months);

        if (months <= 0) months = 6;
        if (months > 24) months = 24;

        // 使用 locator 找到指标
        IndicatorLocator.RecognitionResult recognition = 
            indicatorLocator.recognize(regionName + metricName + "近" + months + "个月");
        
        if (!recognition.isMatched()) {
            return "未找到指标: " + metricName;
        }

        Indicator indicator = recognition.getIndicator();

        Map<String, Object> dims = new HashMap<>();
        dims.put("region", regionName);
        dims.put("time", "last:" + months);
        
        DimensionNormalizationService.NormalizedDimensions normalized = 
            dimensionService.normalize(
                indicator.getIndicatorId(),
                indicator.getTableId(),
                dims
            );

        DataQueryService.QueryResult result = dataQueryService.query(
            indicator.getTableId(),
            normalized
        );

        if (!result.isSuccess() || result.getRows() == null || result.getRows().isEmpty()) {
            return String.format("未找到 %s 在 %s 的历史数据", metricName, regionName);
        }

        List<DataQueryService.DataRow> rows = result.getRows();
        
        // 按时间排序
        rows.sort(Comparator.comparing(DataQueryService.DataRow::getTimeId));

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%s的%s趋势（最近%d个月）：\n\n",
                regionName, metricName, rows.size()));

        // 计算最大值用于图表
        BigDecimal maxValue = rows.stream()
                .map(DataQueryService.DataRow::getValue)
                .filter(Objects::nonNull)
                .max(Comparator.naturalOrder())
                .orElse(BigDecimal.ONE);

        for (DataQueryService.DataRow row : rows) {
            String timeStr = row.getTimeId();
            String bar = generateBar(row.getValue(), maxValue, 15);
            sb.append(String.format("%s: %s %s %s\n",
                    timeStr,
                    row.getValue() != null ? row.getValue().toPlainString() : "N/A",
                    getUnit(metricName),
                    bar));
        }

        // 添加汇总统计
        BigDecimal avgValue = rows.stream()
                .map(DataQueryService.DataRow::getValue)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(rows.size()), 2, RoundingMode.HALF_UP);

        sb.append(String.format("\n平均值: %s %s", avgValue.toPlainString(), getUnit(metricName)));

        return sb.toString();
    }

    /**
     * 查询指标的地区排名
     */
    @Tool(description = "查询指标的地区排名。参数：metricName(指标名称)，topN(排名数量，默认5)")
    public String queryMetricRanking(
            @ToolParam(description = "指标名称，如：招聘数量、平均薪资") String metricName,
            @ToolParam(description = "显示前N名，默认5") int topN) {

        log.info("查询排名: metric={}, topN={}", metricName, topN);

        if (topN <= 0) topN = 5;
        if (topN > 20) topN = 20;

        // 使用 locator 找到指标
        IndicatorLocator.RecognitionResult recognition = 
            indicatorLocator.recognize("各省份" + metricName);
        
        if (!recognition.isMatched()) {
            return "未找到指标: " + metricName;
        }

        Indicator indicator = recognition.getIndicator();

        Map<String, Object> dims = new HashMap<>();
        dims.put("region", "省级");  // 触发省级分组排名查询
        dims.put("time", "latest");
        
        DimensionNormalizationService.NormalizedDimensions normalized = 
            dimensionService.normalize(
                indicator.getIndicatorId(),
                indicator.getTableId(),
                dims
            );

        DataQueryService.QueryResult result = dataQueryService.query(
            indicator.getTableId(),
            normalized
        );

        if (!result.isSuccess() || result.getRanking() == null || result.getRanking().isEmpty()) {
            return "未找到排名数据";
        }

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%s地区排名（前%d名）：\n\n", metricName, 
                Math.min(topN, result.getRanking().size())));

        BigDecimal maxValue = result.getRanking().get(0).getValue();
        for (int i = 0; i < Math.min(topN, result.getRanking().size()); i++) {
            DataQueryService.RankingItem item = result.getRanking().get(i);
            String medal = i < 3 ? new String[]{"🥇", "🥈", "🥉"}[i] : String.format("%d.", i + 1);
            String bar = generateBar(item.getValue(), maxValue, 15);

            sb.append(String.format("%s %s: %s %s %s\n",
                    medal,
                    item.getRegionId(),
                    item.getValue() != null ? item.getValue().toPlainString() : "N/A",
                    getUnit(metricName),
                    bar));
        }

        return sb.toString();
    }

    /**
     * 解析用户输入中的维度信息
     */
    @Tool(description = "从用户输入中提取维度信息。参数：userInput(用户输入文本)")
    public String extractDimensions(@ToolParam(description = "用户输入的查询文本") String userInput) {
        log.info("提取维度: input={}", userInput);

        Set<String> foundMetrics = new HashSet<>();
        Set<String> foundRegions = new HashSet<>();

        // 尝试识别指标
        IndicatorLocator.RecognitionResult recognition = indicatorLocator.recognize(userInput);
        if (recognition.isMatched()) {
            foundMetrics.add(recognition.getIndicator().getIndicatorName());
        }

        // 匹配地区
        for (String key : REGION_NAME_MAP.keySet()) {
            if (userInput.contains(key)) {
                foundRegions.add(key);
            }
        }

        StringBuilder result = new StringBuilder();
        result.append("从输入中提取的信息：\n");
        result.append(String.format("- 指标：%s\n", foundMetrics.isEmpty() ? "未识别" : String.join(", ", foundMetrics)));
        result.append(String.format("- 地区：%s\n", foundRegions.isEmpty() ? "未识别" : String.join(", ", foundRegions)));

        return result.toString();
    }

    // ============ 辅助方法 ============

    private String getUnit(String metricName) {
        if (metricName.contains("薪资") || metricName.contains("工资")) {
            return "元";
        }
        if (metricName.contains("数量") || metricName.contains("企业") || metricName.contains("岗位")) {
            return "个";
        }
        return "";
    }

    private String generateBar(BigDecimal value, BigDecimal max, int maxWidth) {
        if (value == null || max == null || max.compareTo(BigDecimal.ZERO) == 0) {
            return "";
        }
        int width = value.multiply(BigDecimal.valueOf(maxWidth))
                .divide(max, 0, RoundingMode.HALF_UP)
                .intValue();
        return "█".repeat(Math.max(1, width));
    }
}
