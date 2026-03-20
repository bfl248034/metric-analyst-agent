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
 * 数据表登记 - db_data_table
 * 对应 init_mysql.sql 结构
 */
@Data
@Entity
@Table(name = "db_data_table")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DataTable {

    @Id
    @Column(name = "table_id", length = 64)
    private String tableId;

    @Column(name = "table_name", nullable = false, length = 128)
    private String tableName;

    @Column(name = "table_alias", length = 128)
    private String tableAlias;

    @Column(name = "source_id", nullable = false, length = 64)
    private String sourceId;

    @Column(name = "database_name", length = 64)
    private String databaseName;

    @Column(name = "schema_name", length = 64)
    private String schemaName;

    @Column(name = "table_type", length = 20)
    private String tableType;
    // fact(事实表)/dim(维度表)

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "time_column", length = 64)
    private String timeColumn;

    @Column(name = "region_column", length = 64)
    private String regionColumn;

    @Column(name = "value_column", length = 64)
    private String valueColumn;

    @Column(name = "is_active")
    private Boolean isActive;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
