package com.metric.analyst.agent.tools;

import com.metric.analyst.agent.entity.IndicatorFact;
import com.metric.analyst.agent.repository.IndicatorFactRepository;
import com.metric.analyst.agent.repository.IndicatorRepository;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 指标数据查询工具 - Spring AI Tool 注解方式
 */
@Component
public class MetricQueryTools {

    private final IndicatorRepository indicatorRepository;
    private final IndicatorFactRepository factRepository;

    public MetricQueryTools(IndicatorRepository indicatorRepository, 
                           IndicatorFactRepository factRepository) {
        this.indicatorRepository = indicatorRepository;
        this.factRepository = factRepository;
    }

    /**
     * 查询单指标当前值
     */
    @Tool(name = "queryMetricCurrentValue", 
          description = "查询指定指标在当前时间的数值，支持地区筛选")
    public String queryMetricCurrentValue(
            @ToolParam(description = "指标名称，如：招聘数量、专利数量、企业数量") String metricName,
            @ToolParam(description = "地区名称，如：北京、上海、广东、全国") String region) {
        
        var indicatorOpt = indicatorRepository.findAll().stream()
            .filter(i -> i.getName().contains(metricName) || metricName.contains(i.getName()))
            .findFirst();
        
        if (indicatorOpt.isEmpty()) {
            return "未找到指标: " + metricName;
        }
        
        var indicator = indicatorOpt.get();
        String regionCode = resolveRegionCode(region);
        String currentMonth = getCurrentMonth();
        
        var facts = factRepository.findByIndicatorAndRegion(
            indicator.getIndicatorId(), regionCode, currentMonth);
        
        if (facts.isEmpty()) {
            return String.format("未找到 %s 在 %s 的数据", metricName, region);
        }
        
        IndicatorFact fact = facts.get(0);
        return String.format("%s @ %s = %s %s (时间: %s)", 
            indicator.getName(), region, fact.getValue(), indicator.getUnit(), currentMonth);
    }

    /**
     * 查询多地区对比
     */
    @Tool(name = "queryMetricComparison", 
          description = "对比同一指标在多个地区的数值")
    public String queryMetricComparison(
            @ToolParam(description = "指标名称") String metricName,
            @ToolParam(description = "地区列表，逗号分隔，如：北京,上海,广东") String regions) {
        
        var indicatorOpt = indicatorRepository.findAll().stream()
            .filter(i -> i.getName().contains(metricName) || metricName.contains(i.getName()))
            .findFirst();
        
        if (indicatorOpt.isEmpty()) {
            return "未找到指标: " + metricName;
        }
        
        var indicator = indicatorOpt.get();
        String currentMonth = getCurrentMonth();
        String[] regionArray = regions.split(",");
        
        StringBuilder result = new StringBuilder();
        result.append(String.format("%s 地区对比 (%s):\n", indicator.getName(), currentMonth));
        
        for (String region : regionArray) {
            String regionCode = resolveRegionCode(region.trim());
            var facts = factRepository.findByIndicatorAndRegion(
                indicator.getIndicatorId(), regionCode, currentMonth);
            
            if (!facts.isEmpty()) {
                IndicatorFact fact = facts.get(0);
                result.append(String.format("  %s: %s %s (同比: %.2f%%)\n", 
                    region.trim(), fact.getValue(), indicator.getUnit(),
                    fact.getValueYoy().multiply(BigDecimal.valueOf(100))));
            }
        }
        
        return result.toString();
    }

    /**
     * 查询趋势
     */
    @Tool(name = "queryMetricTrend", 
          description = "查询指标的历史趋势")
    public String queryMetricTrend(
            @ToolParam(description = "指标名称") String metricName,
            @ToolParam(description = "地区名称") String region,
            @ToolParam(description = "查询月数，默认6个月") int months) {
        
        var indicatorOpt = indicatorRepository.findAll().stream()
            .filter(i -> i.getName().contains(metricName) || metricName.contains(i.getName()))
            .findFirst();
        
        if (indicatorOpt.isEmpty()) {
            return "未找到指标: " + metricName;
        }
        
        var indicator = indicatorOpt.get();
        String regionCode = resolveRegionCode(region);
        
        var facts = factRepository.findTrendByIndicator(indicator.getIndicatorId(), regionCode)
            .stream()
            .limit(months)
            .sorted(java.util.Comparator.comparing(IndicatorFact::getTimeId))
            .collect(Collectors.toList());
        
        StringBuilder result = new StringBuilder();
        result.append(String.format("%s @ %s 趋势 (%d个月):\n", 
            indicator.getName(), region, months));
        
        for (IndicatorFact fact : facts) {
            result.append(String.format("  %s: %s %s\n", 
                fact.getTimeId(), fact.getValue(), indicator.getUnit()));
        }
        
        return result.toString();
    }

    /**
     * 查询排名
     */
    @Tool(name = "queryMetricRanking", 
          description = "查询指标的地区排名")
    public String queryMetricRanking(
            @ToolParam(description = "指标名称") String metricName,
            @ToolParam(description = "排名数量，默认5") int topN) {
        
        var indicatorOpt = indicatorRepository.findAll().stream()
            .filter(i -> i.getName().contains(metricName) || metricName.contains(i.getName()))
            .findFirst();
        
        if (indicatorOpt.isEmpty()) {
            return "未找到指标: " + metricName;
        }
        
        var indicator = indicatorOpt.get();
        String currentMonth = getCurrentMonth();
        
        var facts = factRepository.findRanking(indicator.getIndicatorId(), currentMonth)
            .stream()
            .filter(f -> !"100000".equals(f.getRegionCode()))
            .limit(topN)
            .collect(Collectors.toList());
        
        StringBuilder result = new StringBuilder();
        result.append(String.format("%s 地区排名 TOP%d (%s):\n", 
            indicator.getName(), topN, currentMonth));
        
        int rank = 1;
        for (IndicatorFact fact : facts) {
            result.append(String.format("  #%d %s: %s %s\n", 
                rank++, getRegionName(fact.getRegionCode()), 
                fact.getValue(), indicator.getUnit()));
        }
        
        return result.toString();
    }

    // 辅助方法
    private String resolveRegionCode(String name) {
        return switch (name) {
            case "全国" -> "100000";
            case "北京" -> "110000";
            case "上海" -> "310000";
            case "广东" -> "440000";
            case "江苏" -> "320000";
            case "浙江" -> "330000";
            default -> "100000";
        };
    }

    private String getRegionName(String code) {
        return switch (code) {
            case "100000" -> "全国";
            case "110000" -> "北京";
            case "310000" -> "上海";
            case "440000" -> "广东";
            case "320000" -> "江苏";
            case "330000" -> "浙江";
            default -> code;
        };
    }

    private String getCurrentMonth() {
        return java.time.LocalDate.now()
            .format(java.time.format.DateTimeFormatter.ofPattern("yyyyMM"));
    }
}
