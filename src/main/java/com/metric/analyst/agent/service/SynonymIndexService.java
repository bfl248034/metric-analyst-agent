package com.metric.analyst.agent.service;

import com.metric.analyst.agent.entity.DimensionValue;
import com.metric.analyst.agent.entity.Indicator;
import com.metric.analyst.agent.repository.DimensionValueRepository;
import com.metric.analyst.agent.repository.IndicatorRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 同义词倒排索引服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SynonymIndexService {

    private final DimensionValueRepository dimensionValueRepository;
    private final IndicatorRepository indicatorRepository;

    // 维度值同义词索引：同义词 -> 维度值编码列表
    private Map<String, List<String>> dimensionSynonymIndex = new HashMap<>();
    
    // 指标同义词索引：同义词 -> 指标ID列表
    private Map<String, List<String>> indicatorSynonymIndex = new HashMap<>();

    @PostConstruct
    public void build() {
        log.info("Building synonym index...");
        
        // 1. 构建维度值同义词索引
        List<DimensionValue> dimensionValues = dimensionValueRepository.findAll();
        for (DimensionValue dv : dimensionValues) {
            if (dv.getSynonyms() != null && !dv.getSynonyms().isEmpty()) {
                String[] synonyms = dv.getSynonyms().split(",");
                for (String synonym : synonyms) {
                    String key = synonym.trim().toLowerCase();
                    dimensionSynonymIndex
                        .computeIfAbsent(key, k -> new ArrayList<>())
                        .add(dv.getValueCode());
                }
            }
            // 同时索引value_name本身
            dimensionSynonymIndex
                .computeIfAbsent(dv.getValueName().toLowerCase(), k -> new ArrayList<>())
                .add(dv.getValueCode());
        }
        
        // 2. 构建指标同义词索引
        List<Indicator> indicators = indicatorRepository.findAll();
        for (Indicator ind : indicators) {
            if (ind.getTags() != null && !ind.getTags().isEmpty()) {
                String[] tags = ind.getTags().split(",");
                for (String tag : tags) {
                    String key = tag.trim().toLowerCase();
                    indicatorSynonymIndex
                        .computeIfAbsent(key, k -> new ArrayList<>())
                        .add(ind.getIndicatorId());
                }
            }
            // 同时索引indicator_name本身
            indicatorSynonymIndex
                .computeIfAbsent(ind.getIndicatorName().toLowerCase(), k -> new ArrayList<>())
                .add(ind.getIndicatorId());
        }
        
        log.info("Built synonym index: {} dimension synonyms, {} indicator synonyms",
            dimensionSynonymIndex.size(), indicatorSynonymIndex.size());
    }

    /**
     * 搜索维度值同义词
     */
    public List<String> searchDimension(String keyword) {
        return dimensionSynonymIndex.getOrDefault(keyword.toLowerCase(), Collections.emptyList());
    }

    /**
     * 搜索指标同义词
     */
    public List<String> searchIndicator(String keyword) {
        return indicatorSynonymIndex.getOrDefault(keyword.toLowerCase(), Collections.emptyList());
    }

    /**
     * 模糊搜索（包含匹配）
     */
    public Map<String, List<String>> fuzzySearchIndicator(String keyword) {
        String lowerKey = keyword.toLowerCase();
        return indicatorSynonymIndex.entrySet().stream()
            .filter(e -> e.getKey().contains(lowerKey))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }
}
