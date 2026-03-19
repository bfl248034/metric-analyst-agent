package com.metric.analyst.agent.service;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.metric.analyst.agent.entity.Indicator;
import com.metric.analyst.agent.repository.IndicatorRepository;
import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 指标向量存储服务 - Java内存实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IndicatorVectorStore {

    private final IndicatorRepository indicatorRepository;
    private final DashScopeChatModel chatModel;
    private final ObjectMapper objectMapper;

    // 内存存储：indicator_id -> embedding_vector
    private Map<String, float[]> embeddings = new ConcurrentHashMap<>();

    @PostConstruct
    public void load() {
        log.info("Loading indicator embeddings into memory...");
        List<Indicator> indicators = indicatorRepository.findByIndexedTrue();
        
        for (Indicator ind : indicators) {
            if (ind.getEmbeddingJson() != null && !ind.getEmbeddingJson().isEmpty()) {
                try {
                    float[] vector = objectMapper.readValue(ind.getEmbeddingJson(), float[].class);
                    embeddings.put(ind.getIndicatorId(), vector);
                } catch (Exception e) {
                    log.warn("Failed to parse embedding for indicator: {}", ind.getIndicatorId());
                }
            }
        }
        log.info("Loaded {} indicator embeddings", embeddings.size());
    }

    /**
     * 向量相似度搜索
     */
    public List<IndicatorScore> searchByVector(String userQuery, int topK) {
        try {
            // 1. 获取用户查询的embedding
            float[] queryVector = embed(userQuery);
            
            // 2. 计算余弦相似度
            return embeddings.entrySet().stream()
                .map(e -> new IndicatorScore(e.getKey(), 
                    cosineSimilarity(queryVector, e.getValue())))
                .sorted(Comparator.comparing(IndicatorScore::getScore).reversed())
                .limit(topK)
                .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Vector search failed", e);
            return Collections.emptyList();
        }
    }

    /**
     * 调用DashScope API获取embedding
     */
    private float[] embed(String text) {
        // TODO: 实现DashScope embedding API调用
        // 临时返回空向量，后续实现
        return new float[1024];
    }

    /**
     * 余弦相似度计算
     */
    private double cosineSimilarity(float[] a, float[] b) {
        double dot = 0, normA = 0, normB = 0;
        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }
        if (normA == 0 || normB == 0) return 0;
        return dot / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    @Data
    public static class IndicatorScore {
        private final String indicatorId;
        private final double score;
    }
}
