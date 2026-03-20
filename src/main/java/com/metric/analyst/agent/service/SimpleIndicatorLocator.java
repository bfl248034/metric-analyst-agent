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

    // 指标名称映射表（与 db_indicator 表中的 indicator_id 对应）
    private static final Map<String, String> METRIC_NAME_MAP = new HashMap<>();
    static {
        // 招聘就业类
        METRIC_NAME_MAP.put("招聘岗位数量", "I_RPA_ICN_RAE_POSITION_NUM");
        METRIC_NAME_MAP.put("招聘数量", "I_RPA_ICN_RAE_POSITION_NUM");
        METRIC_NAME_MAP.put("岗位数量", "I_RPA_ICN_RAE_POSITION_NUM");
        METRIC_NAME_MAP.put("招聘", "I_RPA_ICN_RAE_POSITION_NUM");
        METRIC_NAME_MAP.put("岗位", "I_RPA_ICN_RAE_POSITION_NUM");
        
        METRIC_NAME_MAP.put("招聘岗位平均薪酬", "I_RPA_ICN_RAE_SALARY_AMOUNT");
        METRIC_NAME_MAP.put("平均薪资", "I_RPA_ICN_RAE_SALARY_AMOUNT");
        METRIC_NAME_MAP.put("平均薪酬", "I_RPA_ICN_RAE_SALARY_AMOUNT");
        METRIC_NAME_MAP.put("薪资", "I_RPA_ICN_RAE_SALARY_AMOUNT");
        METRIC_NAME_MAP.put("工资", "I_RPA_ICN_RAE_SALARY_AMOUNT");
        METRIC_NAME_MAP.put("薪酬", "I_RPA_ICN_RAE_SALARY_AMOUNT");
        
        METRIC_NAME_MAP.put("招聘市场主体数量", "I_RPA_ICN_RAE_COMPANY_NUM");
        METRIC_NAME_MAP.put("招聘企业数量", "I_RPA_ICN_RAE_COMPANY_NUM");
        METRIC_NAME_MAP.put("招聘公司数量", "I_RPA_ICN_RAE_COMPANY_NUM");
        
        // 市场主体类
        METRIC_NAME_MAP.put("新增企业数量", "I_RPA_ICN_MKE_COMPANY_ADD_NUM");
        METRIC_NAME_MAP.put("企业新增", "I_RPA_ICN_MKE_COMPANY_ADD_NUM");
        METRIC_NAME_MAP.put("新增企业", "I_RPA_ICN_MKE_COMPANY_ADD_NUM");
        METRIC_NAME_MAP.put("新注册企业", "I_RPA_ICN_MKE_COMPANY_ADD_NUM");
        
        METRIC_NAME_MAP.put("注销企业数量", "I_RPA_ICN_MKE_COMPANY_CANCEL_NUM");
        METRIC_NAME_MAP.put("企业注销", "I_RPA_ICN_MKE_COMPANY_CANCEL_NUM");
        METRIC_NAME_MAP.put("注销企业", "I_RPA_ICN_MKE_COMPANY_CANCEL_NUM");
        
        METRIC_NAME_MAP.put("在营企业数量", "I_RPA_ICN_MKE_COMPANY_ON_NUM");
        METRIC_NAME_MAP.put("企业数量", "I_RPA_ICN_MKE_COMPANY_ON_NUM");
        METRIC_NAME_MAP.put("在营企业", "I_RPA_ICN_MKE_COMPANY_ON_NUM");
        METRIC_NAME_MAP.put("存续企业", "I_RPA_ICN_MKE_COMPANY_ON_NUM");
        
        // 知识产权类
        METRIC_NAME_MAP.put("专利申请数量", "I_RPA_ICN_PAT_APPLICATION_NUM");
        METRIC_NAME_MAP.put("专利申请", "I_RPA_ICN_PAT_APPLICATION_NUM");
        METRIC_NAME_MAP.put("专利", "I_RPA_ICN_PAT_APPLICATION_NUM");
        METRIC_NAME_MAP.put("申请专利", "I_RPA_ICN_PAT_APPLICATION_NUM");
        
        // 政府采购类
        METRIC_NAME_MAP.put("政府采购金额", "I_RPA_ICN_GVP_AMOUNT");
        METRIC_NAME_MAP.put("采购金额", "I_RPA_ICN_GVP_AMOUNT");
        METRIC_NAME_MAP.put("政府支出", "I_RPA_ICN_GVP_AMOUNT");
        
        METRIC_NAME_MAP.put("政府采购数量", "I_RPA_ICN_GVP_NUM");
        METRIC_NAME_MAP.put("采购数量", "I_RPA_ICN_GVP_NUM");
        METRIC_NAME_MAP.put("采购项目数", "I_RPA_ICN_GVP_NUM");
        
        METRIC_NAME_MAP.put("政府采购平均价格", "I_RPA_ICN_GVP_AMOUNT_AVG");
        METRIC_NAME_MAP.put("采购均价", "I_RPA_ICN_GVP_AMOUNT_AVG");
        METRIC_NAME_MAP.put("平均采购价", "I_RPA_ICN_GVP_AMOUNT_AVG");
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
