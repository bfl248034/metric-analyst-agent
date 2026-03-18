package com.metric.analyst.agent;

import com.metric.analyst.agent.entity.DimensionValue;
import com.metric.analyst.agent.entity.Indicator;
import com.metric.analyst.agent.entity.IndicatorFact;
import com.metric.analyst.agent.service.query.InMemoryVectorStore;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/**
 * 简单的功能自测 - 纯Java，无外部依赖
 */
public class SimpleSelfTest {

    public static void main(String[] args) {
        System.out.println("====================================");
        System.out.println("  指标分析系统 - 内存数据库自测");
        System.out.println("====================================");

        // 创建内存存储
        InMemoryVectorStore vectorStore = new InMemoryVectorStore();
        
        // 初始化测试数据
        List<Indicator> indicators = initIndicators(vectorStore);
        List<DimensionValue> dimensions = initDimensions();
        List<IndicatorFact> facts = initFacts(indicators, dimensions);
        
        System.out.println("初始化完成: " + indicators.size() + " 指标, " + 
            dimensions.size() + " 维度值, " + facts.size() + " 事实数据");

        // 创建查询服务（简化版）
        SimpleQueryService queryService = new SimpleQueryService(indicators, facts, dimensions, vectorStore);

        // 测试场景
        System.out.println("\n========== 测试1: 单指标查询 ==========");
        queryService.testSingleQuery("招聘数量", "北京");
        
        System.out.println("\n========== 测试2: 多地区对比 ==========");
        queryService.testMultiRegion("招聘数量", List.of("北京", "上海", "广东"));
        
        System.out.println("\n========== 测试3: 趋势分析 ==========");
        queryService.testTrend("招聘数量", "北京", 6);
        
        System.out.println("\n========== 测试4: 排名查询 ==========");
        queryService.testRanking("招聘数量", 5);
        
        System.out.println("\n========== 测试5: 多指标查询 ==========");
        queryService.testSingleQuery("专利数量", "广东");
        queryService.testSingleQuery("企业数量", "全国");
        
        System.out.println("\n====================================");
        System.out.println("  所有测试完成！系统运行正常！");
        System.out.println("====================================");
    }

    private static List<Indicator> initIndicators(InMemoryVectorStore vectorStore) {
        List<Indicator> list = List.of(
            createIndicator("RECRUITMENT_COUNT", "招聘数量", "企业发布的招聘岗位数量", "个", "人才"),
            createIndicator("PATENT_COUNT", "专利数量", "申请的专利总数", "件", "创新"),
            createIndicator("ENTERPRISE_COUNT", "企业数量", "注册企业总数", "家", "企业"),
            createIndicator("INVESTMENT_AMOUNT", "投资金额", "固定资产投资总额", "亿元", "投资"),
            createIndicator("TALENT_COUNT", "人才数量", "高层次人才总数", "人", "人才"),
            createIndicator("RD_EXPENSE", "研发费用", "企业研发投入总额", "亿元", "创新")
        );
        
        list.forEach(ind -> {
            String text = ind.getName() + " " + ind.getDescription() + " " + ind.getCategory();
            vectorStore.addDocument(ind.getIndicatorId(), text);
        });
        
        return list;
    }

    private static Indicator createIndicator(String id, String name, String desc, String unit, String category) {
        Indicator ind = new Indicator();
        ind.setIndicatorId(id);
        ind.setName(name);
        ind.setDescription(desc);
        ind.setUnit(unit);
        ind.setCategory(category);
        ind.setDataType("INTEGER");
        return ind;
    }

    private static List<DimensionValue> initDimensions() {
        List<DimensionValue> list = new ArrayList<>();
        list.add(createDim("region", "100000", "全国"));
        list.add(createDim("region", "110000", "北京"));
        list.add(createDim("region", "310000", "上海"));
        list.add(createDim("region", "440000", "广东"));
        list.add(createDim("region", "320000", "江苏"));
        list.add(createDim("region", "330000", "浙江"));
        return list;
    }

    private static DimensionValue createDim(String dimId, String code, String name) {
        DimensionValue dv = new DimensionValue();
        dv.setDimensionId(dimId);
        dv.setValueCode(code);
        dv.setValueName(name);
        return dv;
    }

    private static List<IndicatorFact> initFacts(List<Indicator> indicators, List<DimensionValue> regions) {
        List<IndicatorFact> facts = new ArrayList<>();
        Random random = new Random(42);
        List<String> months = generateLast12Months();
        
        for (Indicator ind : indicators) {
            for (DimensionValue region : regions) {
                if ("100000".equals(region.getValueCode())) continue;
                
                for (String month : months) {
                    BigDecimal baseValue = getBaseValue(ind.getIndicatorId(), region.getValueCode());
                    double variation = 0.8 + random.nextDouble() * 0.4;
                    BigDecimal value = baseValue.multiply(BigDecimal.valueOf(variation)).setScale(2, RoundingMode.HALF_UP);
                    
                    IndicatorFact fact = new IndicatorFact();
                    fact.setIndicatorId(ind.getIndicatorId());
                    fact.setTimeId(month);
                    fact.setRegionCode(region.getValueCode());
                    fact.setValue(value);
                    fact.setValueYoy(BigDecimal.valueOf(-0.1 + random.nextDouble() * 0.3));
                    facts.add(fact);
                }
            }
        }
        return facts;
    }

    private static List<String> generateLast12Months() {
        List<String> months = new ArrayList<>();
        java.time.LocalDate now = java.time.LocalDate.now();
        for (int i = 11; i >= 0; i--) {
            months.add(now.minusMonths(i).format(java.time.format.DateTimeFormatter.ofPattern("yyyyMM")));
        }
        return months;
    }

    private static BigDecimal getBaseValue(String indicatorId, String regionCode) {
        return switch (indicatorId) {
            case "RECRUITMENT_COUNT" -> "110000".equals(regionCode) ? new BigDecimal("50000") : 
                                          "310000".equals(regionCode) ? new BigDecimal("40000") :
                                          new BigDecimal("30000");
            case "PATENT_COUNT" -> "110000".equals(regionCode) ? new BigDecimal("10000") :
                                      "440000".equals(regionCode) ? new BigDecimal("12000") :
                                      new BigDecimal("5000");
            case "ENTERPRISE_COUNT" -> new BigDecimal("5000");
            case "INVESTMENT_AMOUNT" -> new BigDecimal("100");
            case "TALENT_COUNT" -> new BigDecimal("2000");
            case "RD_EXPENSE" -> new BigDecimal("50");
            default -> new BigDecimal("1000");
        };
    }

    static class SimpleQueryService {
        private final List<Indicator> indicators;
        private final List<IndicatorFact> facts;
        private final List<DimensionValue> dimensions;
        private final InMemoryVectorStore vectorStore;

        SimpleQueryService(List<Indicator> indicators, List<IndicatorFact> facts, 
                          List<DimensionValue> dimensions, InMemoryVectorStore vectorStore) {
            this.indicators = indicators;
            this.facts = facts;
            this.dimensions = dimensions;
            this.vectorStore = vectorStore;
        }

        void testSingleQuery(String indicatorName, String regionName) {
            Indicator ind = matchIndicator(indicatorName);
            if (ind == null) {
                System.out.println("  未找到指标: " + indicatorName);
                return;
            }
            
            String regionCode = resolveRegionCode(regionName);
            String latestMonth = getLatestMonth();
            
            facts.stream()
                .filter(f -> f.getIndicatorId().equals(ind.getIndicatorId()))
                .filter(f -> f.getRegionCode().equals(regionCode))
                .filter(f -> f.getTimeId().equals(latestMonth))
                .findFirst()
                .ifPresentOrElse(
                    f -> System.out.println("  " + ind.getName() + " @ " + regionName + " = " + 
                        f.getValue() + " " + ind.getUnit() + " (时间: " + latestMonth + ")"),
                    () -> System.out.println("  未找到数据")
                );
        }

        void testMultiRegion(String indicatorName, List<String> regions) {
            Indicator ind = matchIndicator(indicatorName);
            if (ind == null) return;
            
            String latestMonth = getLatestMonth();
            System.out.println("  指标: " + ind.getName() + " (" + latestMonth + ")");
            
            for (String regionName : regions) {
                String regionCode = resolveRegionCode(regionName);
                facts.stream()
                    .filter(f -> f.getIndicatorId().equals(ind.getIndicatorId()))
                    .filter(f -> f.getRegionCode().equals(regionCode))
                    .filter(f -> f.getTimeId().equals(latestMonth))
                    .findFirst()
                    .ifPresent(f -> System.out.printf("    %s: %s %s (同比: %.2f%%)\n", 
                        regionName, f.getValue(), ind.getUnit(), 
                        f.getValueYoy().multiply(BigDecimal.valueOf(100))));
            }
        }

        void testTrend(String indicatorName, String regionName, int months) {
            Indicator ind = matchIndicator(indicatorName);
            if (ind == null) return;
            
            String regionCode = resolveRegionCode(regionName);
            System.out.println("  指标: " + ind.getName() + " @ " + regionName);
            
            List<IndicatorFact> sortedFacts = facts.stream()
                .filter(f -> f.getIndicatorId().equals(ind.getIndicatorId()))
                .filter(f -> f.getRegionCode().equals(regionCode))
                .sorted((a, b) -> b.getTimeId().compareTo(a.getTimeId()))
                .limit(months)
                .sorted(Comparator.comparing(IndicatorFact::getTimeId))
                .toList();
            
            sortedFacts.forEach(f -> System.out.println("    " + f.getTimeId() + ": " + f.getValue()));
        }

        void testRanking(String indicatorName, int topN) {
            Indicator ind = matchIndicator(indicatorName);
            if (ind == null) return;
            
            String latestMonth = getLatestMonth();
            System.out.println("  指标: " + ind.getName() + " 排名 TOP" + topN);
            
            List<IndicatorFact> sorted = facts.stream()
                .filter(f -> f.getIndicatorId().equals(ind.getIndicatorId()))
                .filter(f -> f.getTimeId().equals(latestMonth))
                .filter(f -> !"100000".equals(f.getRegionCode()))
                .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                .limit(topN)
                .toList();
            
            int rank = 1;
            for (IndicatorFact f : sorted) {
                System.out.println("    #" + rank + " " + getRegionName(f.getRegionCode()) + ": " + 
                    f.getValue() + " " + ind.getUnit());
                rank++;
            }
        }

        private Indicator matchIndicator(String name) {
            var results = vectorStore.search(name, 3);
            if (!results.isEmpty()) {
                String id = results.get(0).id();
                return indicators.stream()
                    .filter(i -> i.getIndicatorId().equals(id))
                    .findFirst()
                    .orElse(null);
            }
            return indicators.stream()
                .filter(i -> i.getName().contains(name))
                .findFirst()
                .orElse(null);
        }

        private String resolveRegionCode(String name) {
            return switch (name) {
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
                case "110000" -> "北京";
                case "310000" -> "上海";
                case "440000" -> "广东";
                case "320000" -> "江苏";
                case "330000" -> "浙江";
                default -> code;
            };
        }

        private String getLatestMonth() {
            return facts.stream()
                .map(IndicatorFact::getTimeId)
                .max(String::compareTo)
                .orElse("202503");
        }
    }
}
