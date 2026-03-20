package com.metric.analyst.agent.service.datasource;

import com.metric.analyst.agent.entity.DataDimension;
import com.metric.analyst.agent.entity.DataTable;
import com.metric.analyst.agent.entity.DimensionValue;
import com.metric.analyst.agent.service.DimensionNormalizationService;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 动态 SQL 构建器 - 调试版本（字符串拼接，方便查看完整 SQL）
 * ⚠️ 注意：此版本存在 SQL 注入风险，仅用于调试
 */
@Slf4j
@Component
public class DynamicQueryBuilder {

    /**
     * 构建查询 SQL（字符串拼接方式，方便调试）
     * 
     * @param dataTable 数据表配置
     * @param dimensions 维度定义列表
     * @param normalizedDimensions 标准化的维度值
     * @return SQL 构建结果（可直接执行的完整 SQL）
     */
    public SqlBuildResult buildQuerySql(
            DataTable dataTable,
            List<DataDimension> dimensions,
            DimensionNormalizationService.NormalizedDimensions normalizedDimensions) {
        
        StringBuilder sql = new StringBuilder();
        
        // SELECT 部分
        sql.append("SELECT ");
//           .append(dataTable.getTimeColumn()).append(", ");
        
        // 添加地区列
//        if (dataTable.getRegionColumn() != null) {
//            sql.append(dataTable.getRegionColumn()).append(", ");
//        }
        
        // 添加所有维度列（使用 dimension_code）
        for (DataDimension dim : dimensions) {
            if (dim.getDimensionCode() != null && !dim.getDimensionCode().isEmpty()) {
                sql.append(dim.getDimensionCode()).append(", ");
            }
        }
        
        // 添加值列
        sql.append(dataTable.getValueColumn());
        
        // FROM 部分
        sql.append(" FROM ").append(dataTable.getTableName());
        
        // WHERE 部分
        sql.append(" WHERE 1=1");
        
        // 处理时间维度
        DimensionValue timeDim = normalizedDimensions.getDimensionValue("time");
        if (timeDim != null && timeDim.getValueCode() != null) {
            String timeCode = timeDim.getValueCode();
            
            if ("latest".equals(timeCode)) {
                // 查询最新时间
                sql.append(" AND ").append(dataTable.getTimeColumn()).append(" = (")
                   .append("SELECT MAX(").append(dataTable.getTimeColumn()).append(") ")
                   .append("FROM ").append(dataTable.getTableName()).append(")");
            } else if (timeCode.startsWith("last:")) {
                // 近 N 期 - 使用子查询
                int n = Integer.parseInt(timeCode.substring(5));
                sql.append(" AND ").append(dataTable.getTimeColumn()).append(" IN (")
                   .append("SELECT DISTINCT ").append(dataTable.getTimeColumn()).append(" ")
                   .append("FROM ").append(dataTable.getTableName()).append(" ")
                   .append("ORDER BY ").append(dataTable.getTimeColumn()).append(" DESC ")
                   .append("LIMIT ").append(n).append(")");
            } else {
                // 具体时间 - 直接拼接（注意：这里假设 timeCode 是安全的）
                sql.append(" AND ").append(dataTable.getTimeColumn()).append(" = '").append(escapeSql(timeCode)).append("'");
            }
        }
        
        // 处理其他维度（使用 dimension_code 映射到实际字段）
        Map<String, DataDimension> dimConfigMap = dimensions.stream()
            .collect(Collectors.toMap(DataDimension::getDimensionId, d -> d, (a, b) -> a));
        
        Map<String, DimensionValue> allDims = new HashMap<>();
        allDims.putAll(normalizedDimensions.getExplicitDimensions());
        allDims.putAll(normalizedDimensions.getImplicitDimensions());
        
        for (Map.Entry<String, DimensionValue> entry : allDims.entrySet()) {
            String dimId = entry.getKey();
            DimensionValue dimValue = entry.getValue();
            
            if ("time".equals(dimId)) {
                continue; // 时间已处理
            }
            
            // 获取维度配置，使用 dimension_code 作为字段名
            DataDimension dimConfig = dimConfigMap.get(dimId);
            if (dimConfig == null || dimConfig.getDimensionCode() == null) {
                continue;
            }
            
            String columnName = dimConfig.getDimensionCode();
            String valueCode = dimValue.getValueCode();
            
            // 地区特殊处理：级别查询 vs 具体编码
            if ("region".equals(dimId)) {
                if ("省级".equals(valueCode) || "市级".equals(valueCode) || "全国".equals(valueCode)) {
                    // 级别查询 - 跳过 WHERE，由 GROUP BY 处理
                    continue;
                }
            }
            
            // 排除 TOTAL（多值查询）
            if ("TOTAL".equals(valueCode)) {
                sql.append(" AND ").append(columnName).append(" != 'TOTAL'");
            } else {
                // 直接拼接值（注意：这里假设 valueCode 是安全的）
                sql.append(" AND ").append(columnName).append(" = '").append(escapeSql(valueCode)).append("'");
            }
        }
        
        // ORDER BY
        sql.append(" ORDER BY ").append(dataTable.getTimeColumn());
        
        String finalSql = sql.toString();
        log.info("Generated SQL: {}", finalSql);
        
        return new SqlBuildResult(finalSql, Collections.emptyList());
    }
    
    /**
     * 构建排名查询 SQL（地区级别分组）
     */
    public SqlBuildResult buildRankingSql(
            DataTable dataTable,
            List<DataDimension> dimensions,
            String regionLevel,
            int topN) {
        
        StringBuilder sql = new StringBuilder();
        
        sql.append("SELECT ")
           .append(dataTable.getRegionColumn()).append(", ")
           .append("SUM(").append(dataTable.getValueColumn()).append(") as total_value ")
           .append("FROM ").append(dataTable.getTableName()).append(" ")
           .append("WHERE ").append(dataTable.getTimeColumn()).append(" = (")
           .append("SELECT MAX(").append(dataTable.getTimeColumn()).append(") FROM ")
           .append(dataTable.getTableName()).append(")");
        
        // 根据地区级别筛选
        if ("省级".equals(regionLevel)) {
            sql.append(" AND (").append(dataTable.getRegionColumn()).append(" LIKE '__0000' OR ")
               .append(dataTable.getRegionColumn()).append(" = '100000')");
        } else if ("市级".equals(regionLevel)) {
            sql.append(" AND ").append(dataTable.getRegionColumn()).append(" LIKE '____00' AND ")
               .append(dataTable.getRegionColumn()).append(" != '100000'");
        }
        
        // 排除 TOTAL
        sql.append(" AND ").append(dataTable.getRegionColumn()).append(" != 'TOTAL'");
        
        sql.append(" GROUP BY ").append(dataTable.getRegionColumn()).append(" ")
           .append("ORDER BY total_value DESC ")
           .append("LIMIT ").append(topN);
        
        String finalSql = sql.toString();
        log.info("Generated Ranking SQL: {}", finalSql);
        
        return new SqlBuildResult(finalSql, Collections.emptyList());
    }
    
    /**
     * 简单的 SQL 转义（仅处理单引号）
     * ⚠️ 这不是完整的 SQL 注入防护
     */
    private String escapeSql(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("'", "''");
    }
    
    /**
     * SQL 构建结果
     */
    @Data
    @Builder
    public static class SqlBuildResult {
        private final String sql;
        private final List<Object> params;  // 调试版本此列表始终为空
        
        public Object[] getParamsArray() {
            return params.toArray();
        }
    }
}
