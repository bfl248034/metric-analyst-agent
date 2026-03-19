package com.metric.analyst.agent.agents;

import com.metric.analyst.agent.dto.MetricComparisonDTO;
import com.metric.analyst.agent.dto.MetricRankingDTO;
import com.metric.analyst.agent.dto.MetricTrendDTO;
import com.metric.analyst.agent.entity.IndicatorFact;
import com.metric.analyst.agent.repository.IndicatorFactRepository;
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

    private final IndicatorFactRepository factRepository;

    // 指标名称映射
    private static final Map<String, String> METRIC_NAME_MAP = Map.ofEntries(
            Map.entry("招聘数量", "recruitment_count"),
            Map.entry("平均薪资", "avg_salary"),
            Map.entry("招聘", "recruitment_count"),
            Map.entry("薪资", "avg_salary"),
            Map.entry("人才需求", "talent_demand_index"),
            Map.entry("薪资增长", "salary_growth_rate")
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

        String metricCode = normalizeMetricName(metricName);
        String regionCode = normalizeRegionName(regionName);

        if (metricCode == null) {
            return "未找到指标: " + metricName + "。可用指标：招聘数量、平均薪资";
        }
        if (regionCode == null) {
            return "未找到地区: " + regionName + "。可用地区：北京、上海、广州、深圳、杭州等";
        }

        // 查询最新月份的数据
        List<IndicatorFact> facts = factRepository.findByIndicatorCodeAndRegionCodeOrderByYearDescMonthDesc(
                metricCode, regionCode);

        if (facts.isEmpty()) {
            return String.format("未找到 %s 在 %s 的数据", metricName, regionName);
        }

        IndicatorFact latest = facts.get(0);
        StringBuilder result = new StringBuilder();
        result.append(String.format("%s%s的%s数据：\n", regionName, latest.getYear(), metricName));
        result.append(String.format("- 数值：%s %s\n",
                latest.getMetricValue() != null ? latest.getMetricValue().toPlainString() : "N/A",
                getUnit(metricCode)));

        if (latest.getValueYoy() != null) {
            result.append(String.format("- 同比增长：%.1f%%\n", latest.getValueYoy()));
        }
        if (latest.getValueMom() != null) {
            result.append(String.format("- 环比增长：%.1f%%\n", latest.getValueMom()));
        }

        return result.toString();
    }

    /**
     * 对比多个地区的同一指标
     */
    @Tool(description = "对比多个地区的同一指标。参数：metricName(指标名称)，regionNames(地区名称列表，用逗号分隔)")
    public String queryMetricComparison(
            @ToolParam(description = "指标名称，如：招聘数量、平均薪资") String metricName,
            @ToolParam(description = "地区名称列表，用逗号分隔，如：北京,上海,杭州") String regionNames) {

        log.info("对比指标: metric={}, regions={}", metricName, regionNames);

        String metricCode = normalizeMetricName(metricName);
        if (metricCode == null) {
            return "未找到指标: " + metricName;
        }

        String[] regions = regionNames.split("[，,]");
        List<MetricComparisonDTO.RegionData> dataList = new ArrayList<>();

        for (String region : regions) {
            String regionName = region.trim();
            String regionCode = normalizeRegionName(regionName);
            if (regionCode == null) continue;

            List<IndicatorFact> facts = factRepository
                    .findByIndicatorCodeAndRegionCodeOrderByYearDescMonthDesc(metricCode, regionCode);
            if (!facts.isEmpty()) {
                IndicatorFact fact = facts.get(0);
                dataList.add(new MetricComparisonDTO.RegionData(
                        regionName, fact.getMetricValue(), fact.getValueYoy()));
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
                    getUnit(metricCode),
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
        if (months > 24) months = 24; // 最多24个月

        String metricCode = normalizeMetricName(metricName);
        String regionCode = normalizeRegionName(regionName);

        if (metricCode == null) {
            return "未找到指标: " + metricName;
        }
        if (regionCode == null) {
            return "未找到地区: " + regionName;
        }

        List<IndicatorFact> facts = factRepository
                .findByIndicatorCodeAndRegionCodeOrderByYearDescMonthDesc(metricCode, regionCode);

        if (facts.isEmpty()) {
            return String.format("未找到 %s 在 %s 的历史数据", metricName, regionName);
        }

        // 取最近N个月
        List<IndicatorFact> recentFacts = facts.stream()
                .limit(months)
                .sorted(Comparator.comparing(IndicatorFact::getYear)
                        .thenComparing(IndicatorFact::getMonth))
                .collect(Collectors.toList());

        StringBuilder result = new StringBuilder();
        result.append(String.format("%s%s的%s趋势（最近%d个月）：\n\n",
                regionName, recentFacts.get(0).getYear(), metricName, recentFacts.size()));

        // 计算最大值用于图表
        BigDecimal maxValue = recentFacts.stream()
                .map(IndicatorFact::getMetricValue)
                .filter(Objects::nonNull)
                .max(Comparator.naturalOrder())
                .orElse(BigDecimal.ONE);

        for (IndicatorFact fact : recentFacts) {
            String monthStr = String.format("%02d", fact.getMonth());
            String bar = generateBar(fact.getMetricValue(), maxValue, 15);
            result.append(String.format("%s月: %s %s %s",
                    monthStr,
                    fact.getMetricValue() != null ? fact.getMetricValue().toPlainString() : "N/A",
                    getUnit(metricCode),
                    bar));
            if (fact.getValueMom() != null) {
                String momSymbol = fact.getValueMom().compareTo(BigDecimal.ZERO) > 0 ? "↑" : "↓";
                result.append(String.format(" (%s%.1f%%)", momSymbol, fact.getValueMom().abs().doubleValue()));
            }
            result.append("\n");
        }

        // 添加汇总统计
        BigDecimal avgValue = recentFacts.stream()
                .map(IndicatorFact::getMetricValue)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(recentFacts.size()), 2, RoundingMode.HALF_UP);

        result.append(String.format("\n平均值: %s %s", avgValue.toPlainString(), getUnit(metricCode)));

        return result.toString();
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

        String metricCode = normalizeMetricName(metricName);
        if (metricCode == null) {
            return "未找到指标: " + metricName;
        }

        List<MetricRankingDTO.RankingItem> items = new ArrayList<>();

        for (Map.Entry<String, String> entry : REGION_NAME_MAP.entrySet()) {
            String regionName = entry.getKey();
            String regionCode = entry.getValue();

            List<IndicatorFact> facts = factRepository
                    .findByIndicatorCodeAndRegionCodeOrderByYearDescMonthDesc(metricCode, regionCode);
            if (!facts.isEmpty()) {
                IndicatorFact fact = facts.get(0);
                items.add(new MetricRankingDTO.RankingItem(regionName, fact.getMetricValue(), fact.getValueYoy()));
            }
        }

        if (items.isEmpty()) {
            return "未找到排名数据";
        }

        // 按数值排序
        items.sort((a, b) -> b.getValue().compareTo(a.getValue()));

        StringBuilder result = new StringBuilder();
        result.append(String.format("%s地区排名（前%d名）：\n\n", metricName, Math.min(topN, items.size())));

        BigDecimal maxValue = items.get(0).getValue();
        for (int i = 0; i < Math.min(topN, items.size()); i++) {
            MetricRankingDTO.RankingItem item = items.get(i);
            String medal = i < 3 ? new String[]{"🥇", "🥈", "🥉"}[i] : String.format("%d.", i + 1);
            String bar = generateBar(item.getValue(), maxValue, 15);

            result.append(String.format("%s %s: %s %s %s\n",
                    medal,
                    item.getRegionName(),
                    item.getValue() != null ? item.getValue().toPlainString() : "N/A",
                    getUnit(metricCode),
                    bar));
        }

        return result.toString();
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

    private String normalizeMetricName(String input) {
        for (Map.Entry<String, String> entry : METRIC_NAME_MAP.entrySet()) {
            if (input.toLowerCase().contains(entry.getKey().toLowerCase())) {
                return entry.getValue();
            }
        }
        return null;
    }

    private String normalizeRegionName(String input) {
        for (Map.Entry<String, String> entry : REGION_NAME_MAP.entrySet()) {
            if (input.contains(entry.getKey())) {
                return entry.getValue();
            }
        }
        return null;
    }

    private String getUnit(String metricCode) {
        return switch (metricCode) {
            case "recruitment_count" -> "个";
            case "avg_salary" -> "元";
            case "talent_demand_index" -> "点";
            case "salary_growth_rate" -> "%";
            default -> "";
        };
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
