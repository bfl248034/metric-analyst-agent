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

    @Column(name = "indicator_id")
    private String indicatorId;

    @Column(name = "time_id")
    private String timeId;

    @Column(name = "region_code")
    private String regionCode;

    @Column(name = "education_code")
    private String educationCode;

    @Column(name = "industry_chain_code")
    private String industryChainCode;

    @Column(name = "company_type_code")
    private String companyTypeCode;

    @Column(name = "value")
    private BigDecimal value;

    @Column(name = "value_yoy")
    private BigDecimal valueYoy;

    @Column(name = "value_mom")
    private BigDecimal valueMom;
}
