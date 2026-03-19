package com.metric.analyst.agent.agents;

import com.metric.analyst.agent.dto.MetricComparisonDTO;
import com.metric.analyst.agent.dto.MetricRankingDTO;
import com.metric.analyst.agent.dto.MetricTrendDTO;
import com.metric.analyst.agent.entity.IndicatorFact;
import com.metric.analyst.agent.service.DataQueryService;
import com.metric.analyst.agent.service.DimensionNormalizationService;
import com.metric.analyst.agent.service.IndicatorRecognitionService;
import lombok.RequiredArgsConstructor;
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
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MetricQueryTools {

    private final IndicatorRecognitionService recognitionService;
    private final DimensionNormalizationService dimensionService;
    private final DataQueryService dataQueryService;

    // 指标名称映射
    private static final Map<String, String> METRIC_NAME_MAP = Map.ofEntries(
            Map.entry("招聘数量", "I_RPA_ICN_RAE_POSITION_NUM"),
            Map.entry("平均薪资", "I_RPA_ICN_RAE_SALARY_AMOUNT"),
            Map.entry("招聘", "I_RPA_ICN_RAE_POSITION_NUM"),
            Map.entry("薪资", "I_RPA_ICN_RAE_SALARY_AMOUNT"),
            Map.entry("企业新增", "I_RPA_ICN_ECO_SPE_COMPANY_ADD_NUM"),
            Map.entry("企业注销", "I_RPA_ICN_ECO_SPE_COMPANY_CANCEL_NUM")
    );

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

        // 使用识别服务找到指标
        IndicatorRecognitionService.RecognitionResult recognition = 
            recognitionService.recognize(regionName + metricName);
        
        if (!recognition.isMatched()) {
            return "未找到指标: " + metricName + "。可用指标：招聘数量、平均薪资";
        }

        // 标准化维度
        Map<String, Object> dims = new HashMap<>();
        dims.put("region", regionName);
        dims.put("time", "latest");
        
        DimensionNormalizationService.NormalizedDimensions normalized = 
            dimensionService.normalize(
                recognition.getIndicator().getIndicatorId(),
                recognition.getIndicator().getTableId(),
                dims
            );

        // 查询数据
        DataQueryService.QueryResult result = dataQueryService.query(
            recognition.getIndicator().getTableId(),
            normalized
        );

        if (!result.isSuccess() || result.getFacts().isEmpty()) {
            return String.format("未找到 %s 在 %s 的数据", metricName, regionName);
        }

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%s的%s数据：\n", regionName, metricName));
        sb.append(String.format("- 数值：%s %s\n",
                result.getLatestValue() != null ? result.getLatestValue().toPlainString() : "N/A",
                getUnit(metricName)));

        if (result.getLatestYoy() != null) {
            sb.append(String.format("- 同比增长：%.1f%%\n", result.getLatestYoy()));
        }
        if (result.getLatestMom() != null) {
            sb.append(String.format("- 环比增长：%.1f%%\n", result.getLatestMom()));
        }

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

        // 使用识别服务找到指标
        IndicatorRecognitionService.RecognitionResult recognition = 
            recognitionService.recognize(regionNames + metricName);
        
        if (!recognition.isMatched()) {
            return "未找到指标: " + metricName;
        }

        String[] regions = regionNames.split("[，,]");
        List<MetricComparisonDTO.RegionData> dataList = new ArrayList<>();

        for (String region : regions) {
            String regionName = region.trim();
            
            Map<String, Object> dims = new HashMap<>();
            dims.put("region", regionName);
            dims.put("time", "latest");
            
            DimensionNormalizationService.NormalizedDimensions normalized = 
                dimensionService.normalize(
                    recognition.getIndicator().getIndicatorId(),
                    recognition.getIndicator().getTableId(),
                    dims
                );

            DataQueryService.QueryResult result = dataQueryService.query(
                recognition.getIndicator().getTableId(),
                normalized
            );

            if (result.isSuccess() && result.getLatestValue() != null) {
                dataList.add(new MetricComparisonDTO.RegionData(
                        regionName, result.getLatestValue(), result.getLatestYoy()));
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

        // 使用识别服务找到指标
        IndicatorRecognitionService.RecognitionResult recognition = 
            recognitionService.recognize(regionName + metricName + "近" + months + "个月");
        
        if (!recognition.isMatched()) {
            return "未找到指标: " + metricName;
        }

        Map<String, Object> dims = new HashMap<>();
        dims.put("region", regionName);
        dims.put("time", "last:" + months);
        
        DimensionNormalizationService.NormalizedDimensions normalized = 
            dimensionService.normalize(
                recognition.getIndicator().getIndicatorId(),
                recognition.getIndicator().getTableId(),
                dims
            );

        DataQueryService.QueryResult result = dataQueryService.query(
            recognition.getIndicator().getTableId(),
            normalized
        );

        if (!result.isSuccess() || result.getFacts().isEmpty()) {
            return String.format("未找到 %s 在 %s 的历史数据", metricName, regionName);
        }

        List<IndicatorFact> facts = result.getFacts().stream()
            .sorted(Comparator.comparing(IndicatorFact::getTimeId))
            .collect(Collectors.toList());

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%s的%s趋势（最近%d个月）：\n\n",
                regionName, metricName, facts.size()));

        // 计算最大值用于图表
        BigDecimal maxValue = facts.stream()
                .map(IndicatorFact::getFactValue)
                .filter(Objects::nonNull)
                .max(Comparator.naturalOrder())
                .orElse(BigDecimal.ONE);

        for (IndicatorFact fact : facts) {
            String timeStr = fact.getTimeId().toString();
            String bar = generateBar(fact.getFactValue(), maxValue, 15);
            sb.append(String.format("%s: %s %s %s",
                    timeStr,
                    fact.getFactValue() != null ? fact.getFactValue().toPlainString() : "N/A",
                    getUnit(metricName),
                    bar));
            if (fact.getValueMom() != null) {
                String momSymbol = fact.getValueMom().compareTo(BigDecimal.ZERO) > 0 ? "↑" : "↓";
                sb.append(String.format(" (%s%.1f%%)", momSymbol, fact.getValueMom().abs().doubleValue()));
            }
            sb.append("\n");
        }

        // 添加汇总统计
        BigDecimal avgValue = facts.stream()
                .map(IndicatorFact::getFactValue)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(facts.size()), 2, RoundingMode.HALF_UP);

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

        // 使用识别服务找到指标 - 地区级别查询
        IndicatorRecognitionService.RecognitionResult recognition = 
            recognitionService.recognize("各省份" + metricName);
        
        if (!recognition.isMatched()) {
            return "未找到指标: " + metricName;
        }

        Map<String, Object> dims = new HashMap<>();
        dims.put("region", "各省份");  // 触发省级分组查询
        dims.put("time", "latest");
        
        DimensionNormalizationService.NormalizedDimensions normalized = 
            dimensionService.normalize(
                recognition.getIndicator().getIndicatorId(),
                recognition.getIndicator().getTableId(),
                dims
            );

        DataQueryService.QueryResult result = dataQueryService.query(
            recognition.getIndicator().getTableId(),
            normalized
        );

        if (!result.isSuccess() || result.getRanking().isEmpty()) {
            return "未找到排名数据";
        }

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%s地区排名（前%d名）：\n\n", metricName, Math.min(topN, result.getRanking().size())));

        BigDecimal maxValue = result.getRanking().get(0).getValue();
        for (int i = 0; i < Math.min(topN, result.getRanking().size()); i++) {
            DataQueryService.QueryResult.RankingItem item = result.getRanking().get(i);
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

        // 匹配指标
        for (String key : METRIC_NAME_MAP.keySet()) {
            if (userInput.contains(key)) {
                foundMetrics.add(key);
            }
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
