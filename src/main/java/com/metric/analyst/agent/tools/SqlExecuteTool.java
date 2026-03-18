package com.metric.analyst.agent.tools;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * SQL 执行工具
 * 
 * 执行指标数据查询
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SqlExecuteTool {

    private final DataQueryService dataQueryService;

    /**
     * 执行单指标查询
     * 
     * @param indicatorId 指标ID
     * @param dimensions 维度条件
     * @return 查询结果
     */
    @Tool(name = "sql_execute", description = "执行指标数据查询")
    public QueryResult executeQuery(
            @ToolParam(description = "指标ID，如RECRUITMENT_COUNT") String indicatorId,
            @ToolParam(description = "维度条件，如{region: '110000', time: 'recent_1'}") Map<String, String> dimensions) {
        
        log.info("[SqlExecute] Querying indicator: {}, dimensions: {}", indicatorId, dimensions);
        
        try {
            return dataQueryService.query(indicatorId, dimensions);
        } catch (Exception e) {
            log.error("[SqlExecute] Query failed", e);
            return new QueryResult(false, null, e.getMessage());
        }
    }

    /**
     * 执行多指标对比查询
     */
    @Tool(name = "sql_execute_multi", description = "执行多指标对比查询")
    public List<QueryResult> executeMultiQuery(
            @ToolParam(description = "指标ID列表") List<String> indicatorIds,
            @ToolParam(description = "维度条件") Map<String, String> dimensions) {
        
        log.info("[SqlExecute] Multi-query: {}, dimensions: {}", indicatorIds, dimensions);
        
        return indicatorIds.stream()
            .map(id -> executeQuery(id, dimensions))
            .toList();
    }

    // 结果对象
    public record QueryResult(
        boolean success,
        Object data,
        String errorMessage
    ) {
        public QueryResult(Object data) {
            this(true, data, null);
        }
    }

    /**
     * 数据查询服务接口（待实现）
     */
    public interface DataQueryService {
        QueryResult query(String indicatorId, Map<String, String> dimensions);
    }
}
