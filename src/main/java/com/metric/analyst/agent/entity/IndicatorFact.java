package com.metric.analyst.agent.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 指标事实数据
 */
@Data
@Entity
@Table(name = "indicator_fact")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IndicatorFact {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "indicator_code")
    private String indicatorCode;

    @Column(name = "region_code")
    private String regionCode;

    @Column(name = "year")
    private Integer year;

    @Column(name = "month")
    private Integer month;

    @Column(name = "education_level_code")
    private String educationLevelCode;

    @Column(name = "company_type_code")
    private String companyTypeCode;

    @Column(name = "metric_value")
    private BigDecimal metricValue;

    @Column(name = "value_yoy")
    private BigDecimal valueYoy;

    @Column(name = "value_mom")
    private BigDecimal valueMom;
}
