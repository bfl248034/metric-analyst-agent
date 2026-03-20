package com.metric.analyst.agent.interceptor;

import com.alibaba.cloud.ai.graph.agent.interceptor.ToolCallRequest;
import com.alibaba.cloud.ai.graph.agent.interceptor.ToolCallResponse;
import com.alibaba.cloud.ai.graph.agent.interceptor.ToolInterceptor;
import com.alibaba.cloud.ai.graph.agent.interceptor.ToolCallHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 性能监控拦截器
 * 
 * 功能：
 * 1. 记录工具调用耗时
 * 2. 监控工具调用成功率
 * 3. 失败时提供降级响应
 */
@Slf4j
@Component
public class PerformanceInterceptor extends ToolInterceptor {

    @Override
    public ToolCallResponse interceptToolCall(ToolCallRequest request, ToolCallHandler handler) {
        String toolName = request.getToolName();
        String toolCallId = request.getToolCallId();
        long startTime = System.currentTimeMillis();

        log.debug("Tool execution started: {} (callId: {})", toolName, toolCallId);

        try {
            // 执行工具调用
            ToolCallResponse response = handler.call(request);
            
            // 记录成功
            long duration = System.currentTimeMillis() - startTime;
            log.info("Tool executed successfully: {} ({}ms)", toolName, duration);
            
            return response;
            
        } catch (Exception e) {
            // 记录失败
            long duration = System.currentTimeMillis() - startTime;
            log.error("Tool execution failed: {} ({}ms) - {}", toolName, duration, e.getMessage());
            
            // 返回降级响应
            return ToolCallResponse.of(
                toolCallId,
                toolName,
                "工具执行失败，请稍后重试。错误: " + e.getMessage()
            );
        }
    }

    @Override
    public String getName() {
        return "performance_monitor";
    }
}
