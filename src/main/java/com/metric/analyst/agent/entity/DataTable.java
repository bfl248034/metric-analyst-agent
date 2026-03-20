package com.metric.analyst.agent.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * 数据表配置表 - db_data_table
 * 每张指标表对应一条记录
 */
@Data
@Entity
@Table(name = "db_data_table")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DataTable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "table_id", unique = true, nullable = false, length = 128)
    private String tableId;

    @Column(name = "indicator_id", nullable = false, length = 64)
    private String indicatorId;

    @Column(name = "source_id", nullable = false, length = 64)
    private String sourceId;

    @Column(name = "table_name", nullable = false, length = 128)
    private String tableName;
    // 实际的物理表名

    @Column(name = "value_column", nullable = false, length = 64)
    private String valueColumn;
    // 存储指标值的列名

    @Column(name = "mom_column", length = 64)
    private String momColumn;
    // 环比列名

    @Column(name = "yoy_column", length = 64)
    private String yoyColumn;
    // 同比列名

    @Column(name = "time_column", nullable = false, length = 64)
    private String timeColumn;
    // 时间维度列名

    @Column(name = "frequency", length = 10)
    private String frequency;
    // M(月)/Q(季)/Y(年)

    @Column(name = "enabled")
    private Boolean enabled;

    @Column(name = "remark", length = 256)
    private String remark;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
