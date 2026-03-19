package com.metric.analyst.agent.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 指标排名 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MetricRankingDTO {

    private String metricName;
    private String unit;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RankingItem {
        private String regionName;
        private BigDecimal value;
        private BigDecimal yoy;
    }
}
