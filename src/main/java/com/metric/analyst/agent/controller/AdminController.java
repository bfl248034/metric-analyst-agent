package com.metric.analyst.agent.controller;

import com.metric.analyst.agent.service.IndicatorVectorStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 管理接口 - 用于系统维护操作
 */
@Slf4j
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final IndicatorVectorStore indicatorVectorStore;

    /**
     * 初始化所有指标的 embedding
     * 调用后会为 db_indicator 表中 embedding_json 为空的指标生成向量
     */
    @PostMapping("/init-embeddings")
    public Map<String, Object> initializeEmbeddings() {
        log.info("Manual trigger: Initialize embeddings");
        
        long startTime = System.currentTimeMillis();
        indicatorVectorStore.initializeEmbeddings();
        long duration = System.currentTimeMillis() - startTime;
        
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "Embedding initialization triggered");
        result.put("durationMs", duration);
        return result;
    }
}
