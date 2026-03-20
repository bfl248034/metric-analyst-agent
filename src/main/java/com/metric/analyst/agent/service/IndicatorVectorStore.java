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

    /**
     * 初始化所有指标的 embedding
     * 为 embedding_json 为空的指标生成向量并保存到数据库
     */
    public void initializeEmbeddings() {
        log.info("Initializing embeddings for all indicators...");
        
        List<Indicator> allIndicators = indicatorRepository.findAll();
        int successCount = 0;
        int failCount = 0;
        
        for (Indicator indicator : allIndicators) {
            // 跳过已有 embedding 的指标
            if (indicator.getEmbeddingJson() != null && !indicator.getEmbeddingJson().isEmpty()) {
                continue;
            }
            
            try {
                // 生成 embedding 文本：指标名称 + 描述 + 标签
                String textToEmbed = buildEmbeddingText(indicator);
                float[] vector = embed(textToEmbed);
                
                // 保存到数据库
                String embeddingJson = objectMapper.writeValueAsString(vector);
                indicator.setEmbeddingJson(embeddingJson);
                indicator.setIndexed(true);
                indicator.setIndexVersion(System.currentTimeMillis());
                indicator.setLastIndexedAt(java.time.LocalDateTime.now());
                indicatorRepository.save(indicator);
                
                // 同时更新内存缓存
                embeddings.put(indicator.getIndicatorId(), vector);
                
                successCount++;
                log.debug("Generated embedding for indicator: {}", indicator.getIndicatorId());
                
                // 避免触发 API 限流
                Thread.sleep(100);
                
            } catch (Exception e) {
                failCount++;
                log.error("Failed to generate embedding for indicator: {}", indicator.getIndicatorId(), e);
            }
        }
        
        log.info("Embedding initialization completed. Success: {}, Failed: {}", successCount, failCount);
    }
    
    /**
     * 构建用于生成 embedding 的文本
     */
    private String buildEmbeddingText(Indicator indicator) {
        StringBuilder sb = new StringBuilder();
        sb.append(indicator.getIndicatorName());
        
        if (indicator.getRemark() != null && !indicator.getRemark().isEmpty()) {
            sb.append(" ").append(indicator.getRemark());
        }
        
        if (indicator.getTags() != null && !indicator.getTags().isEmpty()) {
            sb.append(" ").append(indicator.getTags().replace(",", " "));
        }
        
        return sb.toString();
    }
}
