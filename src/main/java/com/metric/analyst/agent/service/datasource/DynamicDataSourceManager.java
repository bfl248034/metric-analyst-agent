package com.metric.analyst.agent.service.datasource;

import com.metric.analyst.agent.entity.DataSource;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 动态数据源管理器
 * 管理多个数据源的连接池（MySQL、Kylin 等）
 */
@Slf4j
@Component
public class DynamicDataSourceManager {

    // 数据源连接池缓存：sourceId -> HikariDataSource
    private final Map<String, HikariDataSource> dataSourceMap = new ConcurrentHashMap<>();

    /**
     * 获取或创建数据源
     */
    public DataSource getOrCreateDataSource(DataSource config) {
        return dataSourceMap.computeIfAbsent(config.getSourceId(), k -> {
            log.info("Creating datasource for: {} ({})", config.getSourceId(), config.getSourceType());
            return createHikariDataSource(config);
        });
    }

    /**
     * 获取数据库连接
     */
    public Connection getConnection(DataSource config) throws SQLException {
        return getOrCreateDataSource(config).getConnection();
    }

    /**
     * 创建 Hikari 连接池
     */
    private HikariDataSource createHikariDataSource(DataSource config) {
        HikariConfig hikariConfig = new HikariConfig();
        
        hikariConfig.setJdbcUrl(config.getJdbcUrl());
        hikariConfig.setUsername(config.getUsername());
        hikariConfig.setPassword(config.getPassword());
        hikariConfig.setDriverClassName(getDriverClass(config));
        
        // 连接池配置
        hikariConfig.setMaximumPoolSize(config.getPoolSize() != null ? config.getPoolSize() : 10);
        hikariConfig.setMinimumIdle(2);
        hikariConfig.setConnectionTimeout(config.getConnectionTimeout() != null ? 
            config.getConnectionTimeout() : 30000);
        
        // 数据源名称
        hikariConfig.setPoolName("HikariPool-" + config.getSourceId());
        
        // Kylin 特殊配置
        if (DataSource.SourceType.KYLIN.equals(config.getSourceType())) {
            hikariConfig.setConnectionTestQuery("SELECT 1");
            hikariConfig.setValidationTimeout(5000);
        }

        return new HikariDataSource(hikariConfig);
    }

    /**
     * 获取驱动类名
     */
    private String getDriverClass(DataSource config) {
        if (config.getDriverClass() != null && !config.getDriverClass().isEmpty()) {
            return config.getDriverClass();
        }
        
        return switch (config.getSourceType()) {
            case DataSource.SourceType.MYSQL -> "com.mysql.cj.jdbc.Driver";
            case DataSource.SourceType.KYLIN -> "org.apache.kylin.jdbc.Driver";
            case DataSource.SourceType.CLICKHOUSE -> "com.clickhouse.jdbc.ClickHouseDriver";
            case DataSource.SourceType.DORIS -> "com.mysql.cj.jdbc.Driver";  // Doris 兼容 MySQL 协议
            default -> "com.mysql.cj.jdbc.Driver";
        };
    }

    /**
     * 关闭指定数据源
     */
    public void closeDataSource(String sourceId) {
        HikariDataSource ds = dataSourceMap.remove(sourceId);
        if (ds != null && !ds.isClosed()) {
            log.info("Closing datasource: {}", sourceId);
            ds.close();
        }
    }

    /**
     * 应用关闭时清理所有连接池
     */
    @PreDestroy
    public void destroy() {
        log.info("Closing all datasources...");
        dataSourceMap.forEach((id, ds) -> {
            if (!ds.isClosed()) {
                ds.close();
            }
        });
        dataSourceMap.clear();
    }
}
