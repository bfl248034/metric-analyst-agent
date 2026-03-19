package com.metric.analyst.agent.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.metric.analyst.agent.entity.DataDimension;
import com.metric.analyst.agent.entity.Indicator;
import com.metric.analyst.agent.repository.DataDimensionRepository;
import com.metric.analyst.agent.repository.IndicatorRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 指标识别服务 - 两阶段检索+精排
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IndicatorRecognitionService {

    private final IndicatorRepository indicatorRepository;
    private final DataDimensionRepository dataDimensionRepository;
    private final IndicatorVectorStore vectorStore;
    private final SynonymIndexService synonymIndexService;
    private final ChatModel chatModel;
    private final ObjectMapper objectMapper;

    // 置信度阈值
    private static final double CONFIDENCE_THRESHOLD = 0.6;
    private static final double HIGH_CONFIDENCE = 0.8;

    /**
     * 指标识别主流程
     */
    public RecognitionResult recognize(String userInput) {
        try {
            // 第一阶段：召回候选
            List<IndicatorCandidate> candidates = recallCandidates(userInput);
            
            if (candidates.isEmpty()) {
                return RecognitionResult.noMatch("未找到相关指标");
            }

            // 第二阶段：大模型精排
            RankResult rankResult = llmRank(userInput, candidates);
            
            if (rankResult.getConfidence() < CONFIDENCE_THRESHOLD) {
                // 低置信度，返回候选列表让用户选择
                return RecognitionResult.ambiguous(candidates, rankResult);
            }

            // 高置信度匹配成功
            return RecognitionResult.success(
                rankResult.getIndicator(),
                rankResult.getDimensions(),
                rankResult.getConfidence()
            );

        } catch (Exception e) {
            log.error("Indicator recognition failed", e);
            return RecognitionResult.error(e.getMessage());
        }
    }

    /**
     * 第一阶段：召回候选（三路融合）
     */
    private List<IndicatorCandidate> recallCandidates(String userInput) {
        Map<String, Double> scores = new HashMap<>();

        // 1. BM25全文检索 (权重0.3)
        try {
            List<Indicator> bm25Results = indicatorRepository.searchByFulltext(userInput);
            for (int i = 0; i < bm25Results.size(); i++) {
                String id = bm25Results.get(i).getIndicatorId();
                double score = 0.3 * (1.0 - i * 0.1); // 递减得分
                scores.merge(id, score, Double::sum);
            }
        } catch (Exception e) {
            log.warn("BM25 search failed", e);
        }

        // 2. 向量相似度检索 (权重0.5)
        try {
            List<IndicatorVectorStore.IndicatorScore> vectorResults = 
                vectorStore.searchByVector(userInput, 10);
            for (IndicatorVectorStore.IndicatorScore result : vectorResults) {
                double score = 0.5 * result.getScore();
                scores.merge(result.getIndicatorId(), score, Double::sum);
            }
        } catch (Exception e) {
            log.warn("Vector search failed", e);
        }

        // 3. 同义词检索 (权重0.2)
        String[] keywords = userInput.split("\\s+");
        for (String keyword : keywords) {
            List<String> indicatorIds = synonymIndexService.searchIndicator(keyword);
            for (String id : indicatorIds) {
                scores.merge(id, 0.2, Double::sum);
            }
        }

        // 获取Top 5候选
        return scores.entrySet().stream()
            .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
            .limit(5)
            .map(e -> {
                Indicator ind = indicatorRepository.findByIndicatorId(e.getKey())
                    .orElse(null);
                return new IndicatorCandidate(ind, e.getValue());
            })
            .filter(c -> c.getIndicator() != null)
            .collect(Collectors.toList());
    }

    /**
     * 第二阶段：大模型精排
     */
    private RankResult llmRank(String userInput, List<IndicatorCandidate> candidates) {
        // 构建Prompt
        StringBuilder candidateInfo = new StringBuilder();
        for (int i = 0; i < candidates.size(); i++) {
            Indicator ind = candidates.get(i).getIndicator();
            List<DataDimension> dimensions = dataDimensionRepository.findByTableId(ind.getTableId());
            
            candidateInfo.append(String.format("""
                【指标%d】
                名称：%s
                编码：%s
                领域：%s - %s
                描述：%s
                同义词：%s
                支持维度：%s
                \n""",
                i + 1,
                ind.getIndicatorName(),
                ind.getIndicatorId(),
                ind.getDomain(),
                ind.getSubdomain(),
                ind.getRemark(),
                ind.getTags(),
                dimensions.stream().map(DataDimension::getDimensionId).collect(Collectors.joining(", "))
            ));
        }

        String prompt = String.format("""
            你是指标识别专家，从候选指标中选择最匹配用户查询的一个。
            
            【用户输入】
            %s
            
            【候选指标】
            %s
            
            【特殊规则】
            1. 时间格式：YYYY-MM-DD，取频率最后一天
            2. 地区识别：直接识别地区名称（支持别称如"帝都"="北京"），输出标准化名称
            3. 未指定维度使用默认值
            4. 时间处理：
               - 未指定 → 使用"latest"
               - "近N个月" → 使用"last:N"
               - 指定时间 → 使用YYYY-MM-DD
            
            【输出JSON格式】
            {
              "selected_indicator": "指标名称",
              "indicator_id": "指标编码",
              "confidence": 0.95,
              "reason": "选择原因",
              "extracted_dimensions": {
                "region": {"value": "地区名称", "type": "explicit|level"},
                "time": {"type": "latest|last|specific", "value": "值"},
                "education": {"value": "学历"}
              }
            }
            
            如果都不匹配，输出：{"indicator_id": "NONE", "confidence": 0.0}
            """,
            userInput,
            candidateInfo
        );

        try {
            String response = chatModel.call(new Prompt(prompt)).getResult().getOutput().getText();
            JsonNode json = objectMapper.readTree(response);
            
            String indicatorId = json.get("indicator_id").asText();
            double confidence = json.get("confidence").asDouble();
            
            if ("NONE".equals(indicatorId) || confidence < CONFIDENCE_THRESHOLD) {
                return new RankResult(null, null, confidence);
            }

            Indicator selected = indicatorRepository.findByIndicatorId(indicatorId)
                .orElse(null);
            
            Map<String, Object> dimensions = objectMapper.convertValue(
                json.get("extracted_dimensions"), 
                new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {}
            );

            return new RankResult(selected, dimensions, confidence);

        } catch (Exception e) {
            log.error("LLM ranking failed", e);
            // 降级：返回最高分的候选
            return new RankResult(
                candidates.get(0).getIndicator(),
                new HashMap<>(),
                candidates.get(0).getScore()
            );
        }
    }

    // DTO类
    @Data
    public static class IndicatorCandidate {
        private final Indicator indicator;
        private final double score;
    }

    @Data
    public static class RankResult {
        private final Indicator indicator;
        private final Map<String, Object> dimensions;
        private final double confidence;
    }

    @Data
    public static class RecognitionResult {
        private boolean matched;
        private Indicator indicator;
        private Map<String, Object> dimensions;
        private double confidence;
        private List<IndicatorCandidate> candidates;
        private String message;
        private FallbackType fallbackType;
        private String inferredDomain;  // 推断的用户意图领域

        public enum FallbackType {
            NO_MATCH, AMBIGUOUS, ERROR
        }

        public static RecognitionResult success(Indicator indicator, Map<String, Object> dimensions, double confidence) {
            RecognitionResult r = new RecognitionResult();
            r.matched = true;
            r.indicator = indicator;
            r.dimensions = dimensions;
            r.confidence = confidence;
            return r;
        }

        public static RecognitionResult noMatch(String message) {
            RecognitionResult r = new RecognitionResult();
            r.matched = false;
            r.fallbackType = FallbackType.NO_MATCH;
            r.message = message;
            return r;
        }

        public static RecognitionResult ambiguous(List<IndicatorCandidate> candidates, RankResult rankResult) {
            RecognitionResult r = new RecognitionResult();
            r.matched = false;
            r.fallbackType = FallbackType.AMBIGUOUS;
            r.candidates = candidates;
            r.confidence = rankResult.getConfidence();
            return r;
        }

        public static RecognitionResult noMatchWithDomain(String message, String inferredDomain) {
            RecognitionResult r = new RecognitionResult();
            r.matched = false;
            r.fallbackType = FallbackType.NO_MATCH;
            r.message = message;
            r.inferredDomain = inferredDomain;
            return r;
        }

        public static RecognitionResult error(String message) {
            RecognitionResult r = new RecognitionResult();
            r.matched = false;
            r.fallbackType = FallbackType.ERROR;
            r.message = message;
            return r;
        }
    }
}
