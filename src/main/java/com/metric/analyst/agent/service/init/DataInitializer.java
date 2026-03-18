package com.metric.analyst.agent.service.init;

import com.metric.analyst.agent.entity.DimensionValue;
import com.metric.analyst.agent.entity.Indicator;
import com.metric.analyst.agent.entity.IndicatorFact;
import com.metric.analyst.agent.repository.DimensionValueRepository;
import com.metric.analyst.agent.repository.IndicatorFactRepository;
import com.metric.analyst.agent.repository.IndicatorRepository;
import com.metric.analyst.agent.service.query.InMemoryVectorStore;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 数据初始化服务
 * 
 * 启动时初始化内存数据库和向量存储
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private final IndicatorRepository indicatorRepository;
    private final DimensionValueRepository dimensionValueRepository;
    private final IndicatorFactRepository indicatorFactRepository;
    private final InMemoryVectorStore vectorStore;

    private final Random random = new Random(42); // 固定种子保证可重复

    public DataInitializer(IndicatorRepository indicatorRepository,
                          DimensionValueRepository dimensionValueRepository,
                          IndicatorFactRepository indicatorFactRepository,
                          InMemoryVectorStore vectorStore) {
        this.indicatorRepository = indicatorRepository;
        this.dimensionValueRepository = dimensionValueRepository;
        this.indicatorFactRepository = indicatorFactRepository;
        this.vectorStore = vectorStore;
    }

    @Override
    @Transactional
    public void run(String... args) {
        System.out.println("[DataInitializer] Starting data initialization...");
        
        // 清空旧数据
        indicatorFactRepository.deleteAll();
        dimensionValueRepository.deleteAll();
        indicatorRepository.deleteAll();
        vectorStore.clear();
        
        // 初始化指标
        initIndicators();
        
        // 初始化维度
        initDimensions();
        
        // 初始化事实数据
        initFacts();
        
        System.out.println("[DataInitializer] Data initialization completed!");
        System.out.println("  - Indicators: " + indicatorRepository.count());
        System.out.println("  - Dimension Values: " + dimensionValueRepository.count());
        System.out.println("  - Facts: " + indicatorFactRepository.count());
    }

    private void initIndicators() {
        List<Indicator> indicators = List.of(
            Indicator.builder()
                .indicatorId("RECRUITMENT_COUNT")
                .name("招聘数量")
                .description("企业发布的招聘岗位数量，反映用工需求")
                .unit("个")
                .category("人才")
                .dataType("INTEGER")
                .tableName("indicator_fact")
                .build(),
            Indicator.builder()
                .indicatorId("PATENT_COUNT")
                .name("专利数量")
                .description("申请的专利总数，包括发明、实用新型和外观")
                .unit("件")
                .category("创新")
                .dataType("INTEGER")
                .tableName("indicator_fact")
                .build(),
            Indicator.builder()
                .indicatorId("ENTERPRISE_COUNT")
                .name("企业数量")
                .description("注册企业总数")
                .unit("家")
                .category("企业")
                .dataType("INTEGER")
                .tableName("indicator_fact")
                .build(),
            Indicator.builder()
                .indicatorId("INVESTMENT_AMOUNT")
                .name("投资金额")
                .description("固定资产投资总额")
                .unit("亿元")
                .category("投资")
                .dataType("DECIMAL")
                .tableName("indicator_fact")
                .build(),
            Indicator.builder()
                .indicatorId("TALENT_COUNT")
                .name("人才数量")
                .description("高层次人才总数")
                .unit("人")
                .category("人才")
                .dataType("INTEGER")
                .tableName("indicator_fact")
                .build(),
            Indicator.builder()
                .indicatorId("RD_EXPENSE")
                .name("研发费用")
                .description("企业研发投入总额")
                .unit("亿元")
                .category("创新")
                .dataType("DECIMAL")
                .tableName("indicator_fact")
                .build()
        );
        
        indicatorRepository.saveAll(indicators);
        
        // 添加到向量存储
        indicators.forEach(ind -> {
            String text = ind.getName() + " " + ind.getDescription() + " " + ind.getCategory();
            vectorStore.addDocument(ind.getIndicatorId(), text);
        });
        
        log.info("[DataInitializer] Initialized {} indicators", indicators.size());
    }

    private void initDimensions() {
        List<DimensionValue> dimensions = new ArrayList<>();
        
        // 地区维度 - 国标编码（6位）
        dimensions.addAll(List.of(
            createDim("region", "100000", "全国", 0),
            createDim("region", "110000", "北京", 1),
            createDim("region", "310000", "上海", 2),
            createDim("region", "440000", "广东", 3),
            createDim("region", "320000", "江苏", 4),
            createDim("region", "330000", "浙江", 5)
        ));
        
        // 学历维度
        dimensions.addAll(List.of(
            createDim("education", "0", "不限", 0),
            createDim("education", "1", "博士", 1),
            createDim("education", "2", "硕士", 2),
            createDim("education", "3", "本科", 3),
            createDim("education", "4", "大专", 4),
            createDim("education", "5", "高中及以下", 5)
        ));
        
        // 产业链维度
        dimensions.addAll(List.of(
            createDim("industry_chain", "all", "全部", 0),
            createDim("industry_chain", "material", "材料", 1),
            createDim("industry_chain", "component", "零部件", 2),
            createDim("industry_chain", "assembly", "整机装配", 3),
            createDim("industry_chain", "sales", "销售服务", 4)
        ));
        
        // 企业类型
        dimensions.addAll(List.of(
            createDim("company_type", "all", "全部", 0),
            createDim("company_type", "enterprise", "企业", 1),
            createDim("company_type", "research", "研究机构", 2),
            createDim("company_type", "other", "其他", 3)
        ));
        
        dimensionValueRepository.saveAll(dimensions);
        log.info("[DataInitializer] Initialized {} dimension values", dimensions.size());
    }

    private void initFacts() {
        List<Indicator> indicators = indicatorRepository.findAll();
        List<DimensionValue> regions = dimensionValueRepository.findByDimensionId("region");
        List<String> timeIds = generateLast12Months();
        
        List<IndicatorFact> facts = new ArrayList<>();
        
        for (Indicator indicator : indicators) {
            for (DimensionValue region : regions) {
                for (String timeId : timeIds) {
                    // 全国数据是各省汇总，这里简化处理
                    if ("100000".equals(region.getValueCode()) && !"100000".equals(region.getValueCode())) {
                        continue;
                    }
                    
                    BigDecimal baseValue = getBaseValue(indicator.getIndicatorId(), region.getValueCode());
                    BigDecimal value = generateValue(baseValue, timeId);
                    
                    facts.add(IndicatorFact.builder()
                        .indicatorId(indicator.getIndicatorId())
                        .timeId(timeId)
                        .regionCode(region.getValueCode())
                        .educationCode("0")  // 默认不限
                        .industryChainCode("all")
                        .companyTypeCode("all")
                        .value(value)
                        .valueYoy(generateYoy())
                        .valueMom(generateMom())
                        .build());
                }
            }
        }
        
        indicatorFactRepository.saveAll(facts);
        log.info("[DataInitializer] Initialized {} facts", facts.size());
    }

    private DimensionValue createDim(String dimId, String code, String name, int order) {
        return DimensionValue.builder()
            .dimensionId(dimId)
            .valueCode(code)
            .valueName(name)
            .sortOrder(order)
            .build();
    }

    private List<String> generateLast12Months() {
        List<String> months = new ArrayList<>();
        LocalDate now = LocalDate.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMM");
        
        for (int i = 11; i >= 0; i--) {
            months.add(now.minusMonths(i).format(formatter));
        }
        return months;
    }

    private BigDecimal getBaseValue(String indicatorId, String regionCode) {
        // 根据不同指标和地区设置基础值
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

    private BigDecimal generateValue(BigDecimal baseValue, String timeId) {
        // 添加随机波动和趋势
        double variation = 0.8 + random.nextDouble() * 0.4; // 0.8 ~ 1.2
        double trend = 1.0 + (Integer.parseInt(timeId.substring(4, 6)) * 0.01); // 月度轻微增长
        return baseValue.multiply(BigDecimal.valueOf(variation * trend)).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal generateYoy() {
        return BigDecimal.valueOf(-0.1 + random.nextDouble() * 0.3).setScale(4, RoundingMode.HALF_UP);
    }

    private BigDecimal generateMom() {
        return BigDecimal.valueOf(-0.05 + random.nextDouble() * 0.15).setScale(4, RoundingMode.HALF_UP);
    }
}
