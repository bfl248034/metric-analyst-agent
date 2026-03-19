package com.metric.analyst.agent.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 数据维度关联表 - db_data_dimension
 */
@Data
@Entity
@Table(name = "db_data_dimension")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DataDimension {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "table_id", nullable = false, length = 128)
    private String tableId;

    @Column(name = "dimension_id", nullable = false, length = 64)
    private String dimensionId;

    @Column(name = "dimension_name", length = 64)
    private String dimensionName;

    @Column(name = "dimension_code", length = 64)
    private String dimensionCode;

    @Column(name = "is_common")
    private Boolean isCommon;

    @Column(name = "is_required")
    private Boolean isRequired;

    @Column(name = "default_value", length = 64)
    private String defaultValue;

    @Column(name = "dimension_type", length = 20)
    private String dimensionType;

    @Column(name = "sort_order")
    private Integer sortOrder;
}
