package com.metric.analyst.agent.service;

import com.metric.analyst.agent.entity.DataDimension;
import com.metric.analyst.agent.entity.DimensionValue;
import com.metric.analyst.agent.repository.DataDimensionRepository;
import com.metric.analyst.agent.repository.DimensionValueRepository;
import com.metric.analyst.agent.repository.IndicatorFactRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 维度标准化服务 - 处理时间、地区等维度的标准化
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DimensionNormalizationService {

    private final DimensionValueRepository dimensionValueRepository;
    private final DataDimensionRepository dataDimensionRepository;
    private final IndicatorFactRepository indicatorFactRepository;

    // 地区级别关键词
    private static final Map<String, String> REGION_LEVEL_KEYWORDS = Map.of(
        "各省份", "省级",
        "各省", "省级",
        "不同省份", "省级",
        "各地区", "省级",
        "各地", "省级",
        "各省市", "省级",
        "各市", "市级",
        "不同城市", "市级",
        "各城市", "市级"
    );

    // 多值展开关键词
    private static final Set<String> MULTI_VALUE_KEYWORDS = Set.of(
        "各", "不同", "各个", "分别"
    );

    /**
     * 标准化所有维度
     */
    public NormalizedDimensions normalize(String indicatorId, String tableId, 
                                          Map<String, Object> extractedDimensions) {
        NormalizedDimensions result = new NormalizedDimensions();
        result.setIndicatorId(indicatorId);
        result.setTableId(tableId);

        // 获取该指标的所有维度定义
        List<DataDimension> dimensionDefs = dataDimensionRepository.findByTableId(tableId);

        for (DataDimension dimDef : dimensionDefs) {
            String dimId = dimDef.getDimensionId();
            Object userValue = extractedDimensions.get(dimId);

            if (userValue != null) {
                // 用户指定了该维度
                DimensionValue normalized = normalizeDimension(dimId, userValue.toString(), dimDef);
                result.getExplicitDimensions().put(dimId, normalized);
            } else {
                // 使用默认值
                DimensionValue defaultValue = getDefaultValue(dimId, dimDef);
                result.getImplicitDimensions().put(dimId, defaultValue);
            }
        }

        return result;
    }

    /**
     * 标准化单个维度
     */
    private DimensionValue normalizeDimension(String dimId, String userValue, DataDimension dimDef) {
        return switch (dimId) {
            case "region" -> normalizeRegion(userValue);
            case "time" -> normalizeTime(userValue, dimDef);
            default -> normalizeGenericDimension(dimId, userValue, dimDef);
        };
    }

    /**
     * 标准化地区维度
     */
    private DimensionValue normalizeRegion(String userValue) {
        // 1. 检查是否是级别查询（各省份/各市）
        for (Map.Entry<String, String> entry : REGION_LEVEL_KEYWORDS.entrySet()) {
            if (userValue.contains(entry.getKey())) {
                return DimensionValue.builder()
                    .dimensionId("region")
                    .valueCode(entry.getValue())  // 省级/市级
                    .valueName(userValue)
                    .build();
            }
        }

        // 2. 具体地区标准化
        final String[] normalizedName = {userValue};
        
        // 查询synonyms匹配
        List<DimensionValue> allRegions = dimensionValueRepository.findByDimensionId("region");
        for (DimensionValue dv : allRegions) {
            if (dv.getSynonyms() != null) {
                String[] synonyms = dv.getSynonyms().split(",");
                for (String syn : synonyms) {
                    if (userValue.contains(syn.trim())) {
                        normalizedName[0] = dv.getValueName();
                        break;
                    }
                }
            }
            if (dv.getValueName().contains(userValue) || userValue.contains(dv.getValueName())) {
                normalizedName[0] = dv.getValueName();
                break;
            }
        }

        // 获取编码
        final String finalNormalizedName = normalizedName[0];
        String code = allRegions.stream()
            .filter(dv -> dv.getValueName().equals(finalNormalizedName))
            .findFirst()
            .map(DimensionValue::getValueCode)
            .orElse(userValue);  // _fallback: 使用原始值

        return DimensionValue.builder()
            .dimensionId("region")
            .valueCode(code)
            .valueName(normalizedName[0])
            .build();
    }

    /**
     * 标准化时间维度
     */
    private DimensionValue normalizeTime(String userValue, DataDimension dimDef) {
        // 解析时间表达式
        if ("latest".equalsIgnoreCase(userValue)) {
            return DimensionValue.builder()
                .dimensionId("time")
                .valueCode("latest")
                .valueName("最新")
                .build();
        }

        if (userValue.startsWith("last:")) {
            int periods = Integer.parseInt(userValue.substring(5));
            return DimensionValue.builder()
                .dimensionId("time")
                .valueCode("last:" + periods)
                .valueName("近" + periods + "期")
                .build();
        }

        // 具体时间格式
        return DimensionValue.builder()
            .dimensionId("time")
            .valueCode(userValue)
            .valueName(userValue)
            .build();
    }

    /**
     * 标准化通用维度
     */
    private DimensionValue normalizeGenericDimension(String dimId, String userValue, DataDimension dimDef) {
        // 查询维度值
        List<DimensionValue> values = dimensionValueRepository.findByDimensionId(dimId);
        
        for (DimensionValue dv : values) {
            // 匹配value_name
            if (dv.getValueName().equals(userValue) || userValue.contains(dv.getValueName())) {
                return dv;
            }
            // 匹配synonyms
            if (dv.getSynonyms() != null) {
                String[] synonyms = dv.getSynonyms().split(",");
                for (String syn : synonyms) {
                    if (userValue.contains(syn.trim())) {
                        return dv;
                    }
                }
            }
        }

        // fallback: 使用原始值
        return DimensionValue.builder()
            .dimensionId(dimId)
            .valueCode(userValue)
            .valueName(userValue)
            .build();
    }

    /**
     * 获取默认值
     */
    private DimensionValue getDefaultValue(String dimId, DataDimension dimDef) {
        String defaultCode = dimDef.getDefaultValue();
        
        if (defaultCode == null) {
            defaultCode = "TOTAL";  // 通用默认值
        }

        // 查询维度值名称
        String valueName = defaultCode;
        List<DimensionValue> values = dimensionValueRepository.findByDimensionId(dimId);
        for (DimensionValue dv : values) {
            if (dv.getValueCode().equals(defaultCode)) {
                valueName = dv.getValueName();
                break;
            }
        }

        return DimensionValue.builder()
            .dimensionId(dimId)
            .valueCode(defaultCode)
            .valueName(valueName)
            .build();
    }

    /**
     * 展开时间列表（近N期）
     */
    public List<String> expandTimePeriods(String tableId, String timeExpression) {
        if (!timeExpression.startsWith("last:")) {
            return List.of(timeExpression);
        }

        int n = Integer.parseInt(timeExpression.substring(5));
        
        // 查询最新时间
        String latestTimeStr = indicatorFactRepository.findMaxTimeIdByTableId(tableId);
        if (latestTimeStr == null) {
            return Collections.emptyList();
        }

        LocalDate latest = LocalDate.parse(latestTimeStr, DateTimeFormatter.ISO_DATE);
        List<String> result = new ArrayList<>();

        for (int i = 0; i < n; i++) {
            LocalDate lastDayOfMonth = latest.with(TemporalAdjusters.lastDayOfMonth());
            result.add(lastDayOfMonth.toString());
            latest = latest.minusMonths(1);
        }

        Collections.reverse(result);
        return result;
    }

    /**
     * 判断是否多值查询
     */
    public boolean isMultiValueQuery(String value) {
        if (value == null) return false;
        return MULTI_VALUE_KEYWORDS.stream().anyMatch(value::contains);
    }

    // DTO
    @Data
    public static class NormalizedDimensions {
        private String indicatorId;
        private String tableId;
        private Map<String, DimensionValue> explicitDimensions = new HashMap<>();   // 用户指定
        private Map<String, DimensionValue> implicitDimensions = new HashMap<>();   // 默认值

        public DimensionValue getDimensionValue(String dimId) {
            if (explicitDimensions.containsKey(dimId)) {
                return explicitDimensions.get(dimId);
            }
            return implicitDimensions.get(dimId);
        }
    }
}
