package com.metric.analyst.agent.controller;

import com.metric.analyst.agent.agents.AnalystSupervisor;
import com.metric.analyst.agent.agents.AnalystSupervisor.AnalystResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.Map;
import java.util.UUID;

/**
 * 分析服务 API
 */
@Slf4j
@RestController
@RequestMapping("/api/analyst")
@RequiredArgsConstructor
public class AnalystController {

    private final AnalystSupervisor analystSupervisor;

    /**
     * 同步查询
     */
    @PostMapping("/query")
    public AnalystResponse query(@RequestBody QueryRequest request) {
        String sessionId = request.sessionId != null ? request.sessionId : generateSessionId();
        log.info("[API] Query: {}, session: {}", request.query, sessionId);
        
        return analystSupervisor.processQuery(request.query, sessionId);
    }

    /**
     * 流式查询
     */
    @PostMapping(value = "/query/stream", produces = "text/event-stream")
    public Flux<String> queryStream(@RequestBody QueryRequest request) {
        String sessionId = request.sessionId != null ? request.sessionId : generateSessionId();
        
        return Flux.create(sink -> {
            analystSupervisor.processQueryStream(request.query, sessionId, chunk -> {
                sink.next(chunk);
            });
        });
    }

    /**
     * 健康检查
     */
    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "UP", "version", "1.0.0");
    }

    private String generateSessionId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    public record QueryRequest(String query, String sessionId) {}
}
