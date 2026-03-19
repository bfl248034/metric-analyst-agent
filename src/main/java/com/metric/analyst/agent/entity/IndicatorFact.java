package com.metric.analyst.agent.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 指标事实表 - 预聚合数据
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

    @Column(name = "time_id")
    private LocalDate timeId;

    @Column(name = "region_id", length = 64)
    private String regionId;

    @Column(name = "region_level", length = 20)
    private String regionLevel;

    @Column(name = "education_id", length = 64)
    private String educationId;

    @Column(name = "economic_type_id", length = 64)
    private String economicTypeId;

    @Column(name = "spe_tag_id", length = 64)
    private String speTagId;

    @Column(name = "patent_type_id", length = 64)
    private String patentTypeId;

    @Column(name = "company_type_id", length = 64)
    private String companyTypeId;

    @Column(name = "price_range_id", length = 64)
    private String priceRangeId;

    @Column(name = "scene_type_id", length = 64)
    private String sceneTypeId;

    @Column(name = "data_attr_id", length = 64)
    private String dataAttrId;

    @Column(name = "icn_chain_area_id", length = 64)
    private String icnChainAreaId;

    @Column(name = "icn_chain_link_id", length = 64)
    private String icnChainLinkId;

    @Column(name = "fact_value", precision = 18, scale = 2)
    private BigDecimal factValue;

    @Column(name = "value_mom", precision = 8, scale = 2)
    private BigDecimal valueMom;

    @Column(name = "value_yoy", precision = 8, scale = 2)
    private BigDecimal valueYoy;

    @Column(name = "table_id", length = 128)
    private String tableId;
}
