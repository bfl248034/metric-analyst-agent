package com.metric.analyst.agent.service.query;

import com.metric.analyst.agent.entity.DimensionValue;
import com.metric.analyst.agent.entity.Indicator;
import com.metric.analyst.agent.entity.IndicatorFact;
import com.metric.analyst.agent.repository.DimensionValueRepository;
import com.metric.analyst.agent.repository.IndicatorFactRepository;
import com.metric.analyst.agent.repository.IndicatorRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 数据查询服务
 */
@Service
public class DataQueryService {

    private final IndicatorRepository indicatorRepository;
    private final IndicatorFactRepository factRepository;
    private final DimensionValueRepository dimensionValueRepository;
    private final InMemoryVectorStore vectorStore;

    public DataQueryService(IndicatorRepository indicatorRepository,
                           IndicatorFactRepository factRepository,
                           DimensionValueRepository dimensionValueRepository,
                           InMemoryVectorStore vectorStore) {
        this.indicatorRepository = indicatorRepository;
        this.factRepository = factRepository;
        this.dimensionValueRepository = dimensionValueRepository;
        this.vectorStore = vectorStore;
    }

    /**
     * 单指标查询
     */
    public QueryResult querySingleMetric(String indicatorName, String regionName, String timeDesc) {
        System.out.println("[Query] Single metric: " + indicatorName + ", region: " + regionName + ", time: " + timeDesc);
        
        // 1. 匹配指标
        Indicator indicator = matchIndicator(indicatorName);
        if (indicator == null) {
            return QueryResult.error("未找到指标: " + indicatorName);
        }
        
        // 2. 解析地区
        String regionCode = resolveRegionCode(regionName);
        
        // 3. 解析时间
        String timeId = resolveTimeId(timeDesc);
        
        // 4. 查询数据
        List<IndicatorFact> facts = factRepository.findByIndicatorAndRegion(
            indicator.getIndicatorId(), regionCode, timeId);
        
        if (facts.isEmpty()) {
            return QueryResult.error("未找到数据");
        }
        
        IndicatorFact fact = facts.get(0);
        
        return QueryResult.success(
            indicator.getName(),
            fact.getValue(),
            indicator.getUnit(),
            Map.of("地区", getRegionName(fact.getRegionCode()), 
                   "时间", formatTimeId(fact.getTimeId()))
        );
    }

    /**
     * 多地区对比查询
     */
    public QueryResult queryMultiRegion(String indicatorName, List<String> regionNames, String timeDesc) {
        System.out.println("[Query] Multi-region: " + indicatorName + ", regions: " + regionNames);
        
        Indicator indicator = matchIndicator(indicatorName);
        if (indicator == null) {
            return QueryResult.error("未找到指标: " + indicatorName);
        }
        
        String timeId = resolveTimeId(timeDesc);
        
        List<RegionCompareItem> items = new ArrayList<>();
        for (String regionName : regionNames) {
            String regionCode = resolveRegionCode(regionName);
            List<IndicatorFact> facts = factRepository.findByIndicatorAndRegion(
                indicator.getIndicatorId(), regionCode, timeId);
            
            if (!facts.isEmpty()) {
                IndicatorFact fact = facts.get(0);
                items.add(new RegionCompareItem(
                    getRegionName(fact.getRegionCode()),
                    fact.getValue(),
                    fact.getValueYoy().multiply(BigDecimal.valueOf(100))
                ));
            }
        }
        
        return QueryResult.compare(indicator.getName(), items);
    }

    /**
     * 趋势分析
     */
    public QueryResult queryTrend(String indicatorName, String regionName, int months) {
        System.out.println("[Query] Trend: " + indicatorName + ", region: " + regionName + ", months: " + months);
        
        Indicator indicator = matchIndicator(indicatorName);
        if (indicator == null) {
            return QueryResult.error("未找到指标: " + indicatorName);
        }
        
        String regionCode = resolveRegionCode(regionName);
        List<IndicatorFact> facts = factRepository.findTrendByIndicator(
            indicator.getIndicatorId(), regionCode);
        
        // 取最近N个月
        List<IndicatorFact> recentFacts = facts.stream()
            .limit(months)
            .sorted(Comparator.comparing(IndicatorFact::getTimeId))
            .collect(Collectors.toList());
        
        List<TrendPoint> points = recentFacts.stream()
            .map(f -> new TrendPoint(f.getTimeId(), f.getValue()))
            .collect(Collectors.toList());
        
        // 计算趋势
        String trend = calculateTrend(recentFacts);
        
        return QueryResult.trend(indicator.getName(), points, trend);
    }

    /**
     * 排名查询
     */
    public QueryResult queryRanking(String indicatorName, String timeDesc, int topN) {
        System.out.println("[Query] Ranking: " + indicatorName + ", topN: " + topN);
        
        Indicator indicator = matchIndicator(indicatorName);
        if (indicator == null) {
            return QueryResult.error("未找到指标: " + indicatorName);
        }
        
        String timeId = resolveTimeId(timeDesc);
        List<IndicatorFact> facts = factRepository.findRanking(indicator.getIndicatorId(), timeId);
        
        // 过滤掉全国数据
        List<IndicatorFact> regionFacts = facts.stream()
            .filter(f -> !"100000".equals(f.getRegionCode()))
            .limit(topN)
            .collect(Collectors.toList());
        
        List<RankItem> items = regionFacts.stream()
            .map(f -> new RankItem(
                getRegionName(f.getRegionCode()),
                f.getValue(),
                regionFacts.indexOf(f) + 1
            ))
            .collect(Collectors.toList());
        
        return QueryResult.ranking(indicator.getName(), items);
    }

    // ========== 辅助方法 ==========

    private Indicator matchIndicator(String name) {
        // 先用向量检索
        List<InMemoryVectorStore.SearchResult> results = vectorStore.search(name, 3);
        if (!results.isEmpty()) {
            String indicatorId = results.get(0).id();
            return indicatorRepository.findByIndicatorId(indicatorId).orElse(null);
        }
        
        // 备用：名称匹配
        return indicatorRepository.findAll().stream()
            .filter(i -> i.getName().contains(name) || name.contains(i.getName()))
            .findFirst()
            .orElse(null);
    }

    private String resolveRegionCode(String name) {
        if (name == null || name.isEmpty()) return "100000";
        
        // 特殊标记
        if (name.contains("全国")) return "100000";
        if (name.contains("北京")) return "110000";
        if (name.contains("上海")) return "310000";
        if (name.contains("广东")) return "440000";
        if (name.contains("江苏")) return "320000";
        if (name.contains("浙江")) return "330000";
        
        return "100000";
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

    private String resolveTimeId(String desc) {
        if (desc == null || desc.isEmpty()) {
            // 返回最近一个月
            return java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMM"));
        }
        
        if (desc.contains("最近") || desc.contains("近")) {
            return java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMM"));
        }
        
        // 解析 2024年1月 格式
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("(20\\d{2})年?(\\d{1,2})月?");
        java.util.regex.Matcher matcher = pattern.matcher(desc);
        if (matcher.find()) {
            return matcher.group(1) + String.format("%02d", Integer.parseInt(matcher.group(2)));
        }
        
        return java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMM"));
    }

    private String formatTimeId(String timeId) {
        return timeId.substring(0, 4) + "年" + timeId.substring(4, 6) + "月";
    }

    private String calculateTrend(List<IndicatorFact> facts) {
        if (facts.size() < 2) return "数据不足";
        
        BigDecimal first = facts.get(0).getValue();
        BigDecimal last = facts.get(facts.size() - 1).getValue();
        
        BigDecimal change = last.subtract(first)
            .divide(first, 4, RoundingMode.HALF_UP)
            .multiply(BigDecimal.valueOf(100));
        
        if (change.compareTo(BigDecimal.valueOf(5)) > 0) return "明显上升↗️";
        if (change.compareTo(BigDecimal.valueOf(-5)) < 0) return "明显下降↘️";
        return "相对平稳➡️";
    }

    // ========== 结果对象 ==========

    public record QueryResult(
        boolean success,
        String indicatorName,
        BigDecimal value,
        String unit,
        Map<String, String> dimensions,
        List<RegionCompareItem> compareItems,
        List<TrendPoint> trendPoints,
        String trend,
        List<RankItem> rankItems,
        String errorMessage
    ) {
        public static QueryResult success(String name, BigDecimal value, String unit, Map<String, String> dims) {
            return new QueryResult(true, name, value, unit, dims, null, null, null, null, null);
        }
        
        public static QueryResult error(String message) {
            return new QueryResult(false, null, null, null, null, null, null, null, null, message);
        }
        
        public static QueryResult compare(String name, List<RegionCompareItem> items) {
            return new QueryResult(true, name, null, null, null, items, null, null, null, null);
        }
        
        public static QueryResult trend(String name, List<TrendPoint> points, String trend) {
            return new QueryResult(true, name, null, null, null, null, points, trend, null, null);
        }
        
        public static QueryResult ranking(String name, List<RankItem> items) {
            return new QueryResult(true, name, null, null, null, null, null, null, items, null);
        }
    }

    public record RegionCompareItem(String region, BigDecimal value, BigDecimal yoy) {}
    public record TrendPoint(String timeId, BigDecimal value) {}
    public record RankItem(String region, BigDecimal value, int rank) {}
}
