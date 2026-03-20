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
 * 对应 init_mysql.sql 结构
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
    // mysql/kylin/api

    @Column(name = "host", length = 256)
    private String host;

    @Column(name = "port")
    private Integer port;

    @Column(name = "database_name", length = 64)
    private String databaseName;

    @Column(name = "username", length = 64)
    private String username;

    @Column(name = "password", length = 256)
    private String password;

    @Column(name = "connection_params", columnDefinition = "TEXT")
    private String connectionParams;
    // JSON格式连接参数

    @Column(name = "is_active")
    private Boolean isActive;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * 构建 JDBC URL
     */
    public String buildJdbcUrl() {
        if ("mysql".equalsIgnoreCase(sourceType)) {
            String base = String.format("jdbc:mysql://%s:%d/%s", host, port, databaseName);
            if (connectionParams != null && !connectionParams.isEmpty()) {
                base += "?" + connectionParams.replaceAll("[{}\"]", "").replace(",", "&");
            }
            return base;
        } else if ("kylin".equalsIgnoreCase(sourceType)) {
            return String.format("jdbc:kylin://%s:%d/%s", host, port, databaseName);
        }
        return connectionParams;
    }

    public static final class SourceType {
        public static final String MYSQL = "mysql";
        public static final String KYLIN = "kylin";
        public static final String API = "api";
    }
}
