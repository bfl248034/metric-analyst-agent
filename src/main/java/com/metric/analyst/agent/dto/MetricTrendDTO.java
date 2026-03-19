package com.metric.analyst.agent.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 指标趋势 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MetricTrendDTO {

    private String metricName;
    private String regionName;
    private String unit;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TrendItem {
        private Integer year;
        private Integer month;
        private BigDecimal value;
        private BigDecimal yoy;
        private BigDecimal mom;
    }
}
