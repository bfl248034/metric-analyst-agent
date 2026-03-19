package com.metric.analyst.agent.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 指标对比 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MetricComparisonDTO {

    private String metricName;
    private String unit;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RegionData {
        private String regionName;
        private BigDecimal value;
        private BigDecimal yoy;
    }
}
