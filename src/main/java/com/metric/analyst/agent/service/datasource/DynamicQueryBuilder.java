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
 * 动态 SQL 构建器
 * 根据 db_data_dimension 配置动态构建查询 SQL
 */
@Slf4j
@Component
public class DynamicQueryBuilder {

    /**
     * 构建查询 SQL（参数化）
     * 
     * @param dataTable 数据表配置
     * @param dimensions 维度定义列表
     * @param normalizedDimensions 标准化的维度值
     * @return SQL 构建结果
     */
    public SqlBuildResult buildQuerySql(
            DataTable dataTable,
            List<DataDimension> dimensions,
            DimensionNormalizationService.NormalizedDimensions normalizedDimensions) {
        
        StringBuilder sql = new StringBuilder();
        List<Object> params = new ArrayList<>();
        
        // SELECT 部分
        sql.append("SELECT ")
           .append(dataTable.getTimeColumn()).append(", ");
        
        // 添加地区列
        if (dataTable.getRegionColumn() != null) {
            sql.append(dataTable.getRegionColumn()).append(", ");
        }
        
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
        DimensionNormalizationService.DimensionValue timeDim = normalizedDimensions.getDimensionValue("time");
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
                   .append("LIMIT ?)");
                params.add(n);
            } else {
                // 具体时间
                sql.append(" AND ").append(dataTable.getTimeColumn()).append(" = ?");
                params.add(timeCode);
            }
        }
        
        // 处理其他维度（使用 dimension_code 映射到实际字段）
        Map<String, DataDimension> dimConfigMap = dimensions.stream()
            .collect(Collectors.toMap(DataDimension::getDimensionId, d -> d, (a, b) -> a));
        
        Map<String, DimensionNormalizationService.DimensionValue> allDims = new HashMap<>();
        allDims.putAll(normalizedDimensions.getExplicitDimensions());
        allDims.putAll(normalizedDimensions.getImplicitDimensions());
        
        for (Map.Entry<String, DimensionNormalizationService.DimensionValue> entry : allDims.entrySet()) {
            String dimId = entry.getKey();
            DimensionNormalizationService.DimensionValue dimValue = entry.getValue();
            
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
                sql.append(" AND ").append(columnName).append(" != ?");
                params.add("TOTAL");
            } else {
                sql.append(" AND ").append(columnName).append(" = ?");
                params.add(valueCode);
            }
        }
        
        // ORDER BY
        sql.append(" ORDER BY ").append(dataTable.getTimeColumn());
        
        return new SqlBuildResult(sql.toString(), params);
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
        List<Object> params = new ArrayList<>();
        
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
           .append("LIMIT ?");
        
        params.add(topN);
        
        return new SqlBuildResult(sql.toString(), params);
    }
    
    /**
     * SQL 构建结果
     */
    @Data
    @Builder
    public static class SqlBuildResult {
        private final String sql;
        private final List<Object> params;
        
        public Object[] getParamsArray() {
            return params.toArray();
        }
    }
}
