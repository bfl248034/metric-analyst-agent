package com.metric.analyst.agent;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Metric Analyst Agent 应用
 * 
 * 基于 Spring AI Alibaba 多智能体架构的指标分析系统
 */
@SpringBootApplication
public class MetricAnalystAgentApplication {

    public static void main(String[] args) {
        SpringApplication.run(MetricAnalystAgentApplication.class, args);
    }
}
