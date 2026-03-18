package com.metric.analyst.agent.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 指标定义
 */
@Data
@Entity
@Table(name = "indicator")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Indicator {

    @Id
    @Column(name = "indicator_id")
    private String indicatorId;

    @Column(name = "name")
    private String name;

    @Column(name = "description")
    private String description;

    @Column(name = "unit")
    private String unit;

    @Column(name = "category")
    private String category;

    @Column(name = "data_type")
    private String dataType;

    @Column(name = "table_name")
    private String tableName;
}
