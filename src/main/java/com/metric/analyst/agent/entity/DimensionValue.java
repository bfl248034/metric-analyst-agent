package com.metric.analyst.agent.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 维度值定义
 */
@Data
@Entity
@Table(name = "dimension_value")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DimensionValue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "dimension_id")
    private String dimensionId;

    @Column(name = "value_code")
    private String valueCode;

    @Column(name = "value_name")
    private String valueName;

    @Column(name = "sort_order")
    private Integer sortOrder;
}
