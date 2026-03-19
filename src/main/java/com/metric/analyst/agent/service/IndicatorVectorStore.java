package com.metric.analyst.agent.service;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.metric.analyst.agent.entity.Indicator;
import com.metric.analyst.agent.repository.IndicatorRepository;
import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

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
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${spring.ai.dashscope.api-key:}")
    private String apiKey;

    private static final String EMBEDDING_API_URL = "https://dashscope.aliyuncs.com/api/v1/services/embeddings/text-embedding/text-embedding";
    private static final String EMBEDDING_MODEL = "text-embedding-v2";

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
        if (apiKey == null || apiKey.isEmpty()) {
            log.warn("DashScope API key not configured, returning zero vector");
            return new float[1536]; // text-embedding-v2 returns 1536 dimensions
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + apiKey);

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", EMBEDDING_MODEL);
            requestBody.put("input", Map.of("texts", List.of(text)));

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(EMBEDDING_API_URL, request, String.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());
                JsonNode embeddingNode = root.path("output").path("embeddings").get(0).path("embedding");
                
                if (embeddingNode.isArray()) {
                    float[] vector = new float[embeddingNode.size()];
                    for (int i = 0; i < embeddingNode.size(); i++) {
                        vector[i] = (float) embeddingNode.get(i).asDouble();
                    }
                    return vector;
                }
            }
            
            log.error("Failed to get embedding from DashScope: {}", response.getBody());
            return new float[1536];
            
        } catch (Exception e) {
            log.error("Error calling DashScope embedding API", e);
            return new float[1536];
        }
    }

    /**
     * 余弦相似度计算
     */
    private double cosineSimilarity(float[] a, float[] b) {
        if (a == null || b == null || a.length != b.length) {
            return 0;
        }
        
        // 如果都是零向量，返回0
        boolean allZeroA = true, allZeroB = true;
        for (float v : a) {
            if (v != 0) { allZeroA = false; break; }
        }
        for (float v : b) {
            if (v != 0) { allZeroB = false; break; }
        }
        if (allZeroA || allZeroB) return 0;
        
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
