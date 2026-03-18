package com.metric.analyst.agent.tools;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 指标检索工具
 * 
 * 根据用户描述，从 Elasticsearch 中检索最匹配的指标
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MetricRetrievalTool {

    private final MetricSearchService metricSearchService;

    /**
     * 根据描述检索指标
     * 
     * @param description 用户描述的指标，如"招聘数量"、"专利数量"
     * @param topK 返回前K个结果
     * @return 匹配的指标列表
     */
    @Tool(name = "metric_retrieval", description = "根据描述检索最匹配的指标")
    public List<MetricMatch> retrieveMetrics(
            @ToolParam(description = "用户描述的指标，如'招聘数量'、'专利数量'") String description,
            @ToolParam(description = "返回前K个结果，默认5") int topK) {
        
        log.info("[MetricRetrieval] Searching for: {}, topK: {}", description, topK);
        
        try {
            return metricSearchService.search(description, topK);
        } catch (Exception e) {
            log.error("[MetricRetrieval] Search failed", e);
            return List.of();
        }
    }

    /**
     * 根据ID获取指标详情
     */
    @Tool(name = "metric_get_by_id", description = "根据指标ID获取指标详情")
    public MetricDetail getMetricById(
            @ToolParam(description = "指标ID，如RECRUITMENT_COUNT") String indicatorId) {
        
        log.info("[MetricRetrieval] Getting metric by id: {}", indicatorId);
        return metricSearchService.getById(indicatorId);
    }

    // 结果对象
    public record MetricMatch(
        String indicatorId,
        String name,
        String description,
        double score
    ) {}

    public record MetricDetail(
        String indicatorId,
        String name,
        String description,
        String unit,
        List<String> supportedDimensions
    ) {}

    /**
     * 检索服务接口（待实现）
     */
    public interface MetricSearchService {
        List<MetricMatch> search(String description, int topK);
        MetricDetail getById(String indicatorId);
    }
}
