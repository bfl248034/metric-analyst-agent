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
 * 数据源配置表 - db_data_source
 * 支持 MySQL、Kylin 等多种数据源
 */
@Data
@Entity
@Table(name = "db_data_source")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DataSource {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "source_id", unique = true, nullable = false, length = 64)
    private String sourceId;

    @Column(name = "source_name", nullable = false, length = 128)
    private String sourceName;

    @Column(name = "source_type", nullable = false, length = 20)
    private String sourceType;
    // MYSQL, KYLIN, CLICKHOUSE, DORIS 等

    @Column(name = "jdbc_url", nullable = false, length = 512)
    private String jdbcUrl;

    @Column(name = "username", length = 64)
    private String username;

    @Column(name = "password", length = 128)
    private String password;

    @Column(name = "driver_class", length = 128)
    private String driverClass;

    @Column(name = "pool_size", nullable = false)
    private Integer poolSize;

    @Column(name = "connection_timeout")
    private Integer connectionTimeout;

    @Column(name = "query_timeout")
    private Integer queryTimeout;

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

    /**
     * 数据源类型常量
     */
    public static final class SourceType {
        public static final String MYSQL = "MYSQL";
        public static final String KYLIN = "KYLIN";
        public static final String CLICKHOUSE = "CLICKHOUSE";
        public static final String DORIS = "DORIS";
    }
}
