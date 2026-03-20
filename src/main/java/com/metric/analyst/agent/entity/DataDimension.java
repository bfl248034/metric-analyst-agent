package com.metric.analyst.agent.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 数据维度关联表 - db_data_dimension
 * 对应 init_mysql.sql 结构
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
    // 数据表中的实际字段名

    @Column(name = "is_common")
    private Boolean isCommon;

    @Column(name = "is_required")
    private Boolean isRequired;

    @Column(name = "default_value", length = 64)
    private String defaultValue;

    @Column(name = "dimension_type", length = 20)
    private String dimensionType;
    // temporal/categorical/numerical

    @Column(name = "sort_order")
    private Integer sortOrder;
}
