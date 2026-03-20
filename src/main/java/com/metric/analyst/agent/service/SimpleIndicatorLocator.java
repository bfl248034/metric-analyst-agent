package com.metric.analyst.agent.service;

import com.metric.analyst.agent.entity.Indicator;
import com.metric.analyst.agent.repository.IndicatorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 简单指标定位器实现 - 不依赖 ChatModel
 * 用于 MetricQueryTools，避免循环依赖
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SimpleIndicatorLocator implements IndicatorLocator {

    private final IndicatorRepository indicatorRepository;

    // 指标名称映射表（内置常用映射）
    private static final Map<String, String> METRIC_NAME_MAP = new HashMap<>();
    static {
        METRIC_NAME_MAP.put("招聘数量", "I_RPA_ICN_RAE_POSITION_NUM");
        METRIC_NAME_MAP.put("平均薪资", "I_RPA_ICN_RAE_SALARY_AMOUNT");
        METRIC_NAME_MAP.put("招聘", "I_RPA_ICN_RAE_POSITION_NUM");
        METRIC_NAME_MAP.put("薪资", "I_RPA_ICN_RAE_SALARY_AMOUNT");
        METRIC_NAME_MAP.put("工资", "I_RPA_ICN_RAE_SALARY_AMOUNT");
        METRIC_NAME_MAP.put("薪酬", "I_RPA_ICN_RAE_SALARY_AMOUNT");
        METRIC_NAME_MAP.put("企业新增", "I_RPA_ICN_ECO_SPE_COMPANY_ADD_NUM");
        METRIC_NAME_MAP.put("企业注销", "I_RPA_ICN_ECO_SPE_COMPANY_CANCEL_NUM");
        METRIC_NAME_MAP.put("企业数量", "I_RPA_ICN_ECO_SPE_COMPANY_TOTAL_NUM");
        METRIC_NAME_MAP.put("新增企业", "I_RPA_ICN_ECO_SPE_COMPANY_ADD_NUM");
        METRIC_NAME_MAP.put("注销企业", "I_RPA_ICN_ECO_SPE_COMPANY_CANCEL_NUM");
        METRIC_NAME_MAP.put("专利", "I_RPA_ICN_IPA_PATENT_APPLY_NUM");
        METRIC_NAME_MAP.put("专利申请", "I_RPA_ICN_IPA_PATENT_APPLY_NUM");
    }

    @Override
    public RecognitionResult recognize(String text) {
        if (text == null || text.isEmpty()) {
            return RecognitionResult.fail("输入为空");
        }

        String textLower = text.toLowerCase();
        
        // 1. 先尝试精确匹配内置映射
        for (Map.Entry<String, String> entry : METRIC_NAME_MAP.entrySet()) {
            if (textLower.contains(entry.getKey().toLowerCase())) {
                Optional<Indicator> indicator = indicatorRepository
                    .findByIndicatorId(entry.getValue());
                if (indicator.isPresent()) {
                    log.debug("Found indicator by keyword '{}' -> {}", entry.getKey(), entry.getValue());
                    return RecognitionResult.success(indicator.get(), entry.getKey());
                }
            }
        }

        // 2. 全文搜索匹配
        List<Indicator> searchResults = searchByKeyword(text);
        if (!searchResults.isEmpty()) {
            Indicator bestMatch = searchResults.get(0);
            return RecognitionResult.success(bestMatch, bestMatch.getIndicatorName());
        }

        // 3. 模糊匹配 - 按名称相似度
        List<Indicator> allIndicators = indicatorRepository.findByIndexedTrue();
        Optional<Indicator> fuzzyMatch = allIndicators.stream()
            .filter(ind -> textLower.contains(ind.getIndicatorName().toLowerCase()) ||
                          (ind.getTags() != null && ind.getTags().toLowerCase().contains(textLower)))
            .findFirst();

        if (fuzzyMatch.isPresent()) {
            return RecognitionResult.success(fuzzyMatch.get(), fuzzyMatch.get().getIndicatorName());
        }

        return RecognitionResult.fail("未找到匹配的指标，可用关键词：招聘数量、平均薪资、企业新增、专利等");
    }

    @Override
    public Optional<Indicator> findByName(String name) {
        // 先查映射表
        String indicatorId = METRIC_NAME_MAP.get(name);
        if (indicatorId != null) {
            return indicatorRepository.findByIndicatorId(indicatorId);
        }

        // 再全文搜索
        List<Indicator> results = searchByKeyword(name);
        if (!results.isEmpty()) {
            return Optional.of(results.get(0));
        }

        // 最后模糊匹配
        return indicatorRepository.findByIndicatorNameContaining(name).stream().findFirst();
    }

    @Override
    public List<Indicator> searchByKeyword(String keyword) {
        try {
            return indicatorRepository.searchByFulltext(keyword);
        } catch (Exception e) {
            log.warn("Fulltext search failed, fallback to fuzzy search: {}", e.getMessage());
            // 降级到模糊搜索
            return indicatorRepository.findByIndicatorNameContaining(keyword);
        }
    }
}
