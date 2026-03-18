package com.metric.analyst.agent.tools;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 维度解析工具
 * 
 * 解析用户查询中的维度值，支持地区、时间、学历等维度
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DimensionParseTool {

    private final DimensionValueService dimensionValueService;

    /**
     * 解析地区维度
     * 
     * @param regionText 地区描述，如"北京"、"上海"、"不同省份"
     * @return 地区编码列表或级别标记
     */
    @Tool(name = "region_parse", description = "解析地区维度，返回6位国标编码或LEVEL标记")
    public RegionResult parseRegion(
            @ToolParam(description = "地区描述，如'北京'、'上海'、'不同省份'") String regionText) {
        
        log.info("[DimensionParse] Parsing region: {}", regionText);
        
        // 检测级别标记
        if (regionText.contains("省份") || regionText.contains("地区")) {
            return new RegionResult("LEVEL_2", "不同省份", List.of(), true);
        }
        if (regionText.contains("市") && !regionText.contains("省份")) {
            return new RegionResult("LEVEL_3", "不同市", List.of(), true);
        }
        
        // 解析具体地区
        List<String> codes = dimensionValueService.resolveRegionCodes(regionText);
        
        return new RegionResult(
            codes.isEmpty() ? null : codes.get(0),
            regionText,
            codes,
            false
        );
    }

    /**
     * 解析时间维度
     */
    @Tool(name = "time_parse", description = "解析时间维度，返回YYYYMM或recent_N格式")
    public TimeResult parseTime(
            @ToolParam(description = "时间描述，如'最近3个月'、'2024年1月'") String timeText) {
        
        log.info("[DimensionParse] Parsing time: {}", timeText);
        
        // 解析最近N期
        if (timeText.contains("最近") || timeText.contains("近")) {
            int num = extractNumber(timeText);
            if (num > 0) {
                return new TimeResult("recent_" + num, "最近" + num + "期", "recent");
            }
        }
        
        // 解析关键词
        Map<String, String> keywords = Map.of(
            "上月", "last_month",
            "上个月", "last_month",
            "今年", "this_year",
            "去年", "last_year",
            "当月", "current",
            "本月", "current"
        );
        
        for (Map.Entry<String, String> entry : keywords.entrySet()) {
            if (timeText.contains(entry.getKey())) {
                return new TimeResult(entry.getValue(), entry.getKey(), "keyword");
            }
        }
        
        // 解析 YYYYMM 格式
        String yearMonth = extractYearMonth(timeText);
        if (yearMonth != null) {
            return new TimeResult(yearMonth, timeText, "absolute");
        }
        
        // 默认返回最近1期
        return new TimeResult("recent_1", "最近1期", "default");
    }

    /**
     * 解析枚举维度（学历、产业链等）
     */
    @Tool(name = "enum_dimension_parse", description = "解析学历、产业链等枚举维度")
    public EnumResult parseEnumDimension(
            @ToolParam(description = "维度ID，如education、industry_chain") String dimensionId,
            @ToolParam(description = "维度值描述") String valueText) {
        
        log.info("[DimensionParse] Parsing {}: {}", dimensionId, valueText);
        
        List<DimensionValue> values = dimensionValueService.findByDimensionId(dimensionId);
        
        // 检测分维度分析
        boolean isFacet = valueText.contains("不同") || valueText.contains("分");
        if (isFacet) {
            List<DimensionValue> validValues = values.stream()
                .filter(v -> !isAggregateValue(v.code()))
                .toList();
            
            return new EnumResult(
                dimensionId,
                validValues.get(0).code(),
                validValues.stream().map(DimensionValue::code).toList(),
                true,
                "facet_all"
            );
        }
        
        // 匹配具体值
        for (DimensionValue dv : values) {
            if (valueText.contains(dv.name()) || dv.name().contains(valueText)) {
                return new EnumResult(dimensionId, dv.code(), List.of(dv.code()), false, "exact");
            }
        }
        
        return null;
    }

    // 结果对象
    public record RegionResult(
        String primaryCode,
        String displayText,
        List<String> allCodes,
        boolean isLevelMarker
    ) {}

    public record TimeResult(
        String timeCode,
        String displayText,
        String type
    ) {}

    public record EnumResult(
        String dimensionId,
        String primaryCode,
        List<String> allCodes,
        boolean isFacet,
        String matchType
    ) {}

    public record DimensionValue(String code, String name) {}

    // 辅助方法
    private int extractNumber(String text) {
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("(\\d+)");
        java.util.regex.Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            return Integer.parseInt(matcher.group(1));
        }
        return 0;
    }

    private String extractYearMonth(String text) {
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("(20\\d{2})年?(\\d{1,2})月?");
        java.util.regex.Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            String year = matcher.group(1);
            String month = String.format("%02d", Integer.parseInt(matcher.group(2)));
            return year + month;
        }
        return null;
    }

    private boolean isAggregateValue(String code) {
        return code.equals("all") || code.equals("total") || 
               code.equals("不限") || code.equals("全部");
    }

    /**
     * 维度值服务接口（待实现）
     */
    public interface DimensionValueService {
        List<String> resolveRegionCodes(String regionText);
        List<DimensionValue> findByDimensionId(String dimensionId);
    }
}
