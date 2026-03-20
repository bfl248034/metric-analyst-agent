package com.metric.analyst.agent.service;

import com.metric.analyst.agent.entity.DataDimension;
import com.metric.analyst.agent.entity.DataSource;
import com.metric.analyst.agent.entity.DataTable;
import com.metric.analyst.agent.repository.DataDimensionRepository;
import com.metric.analyst.agent.repository.DataSourceRepository;
import com.metric.analyst.agent.repository.DataTableRepository;
import com.metric.analyst.agent.service.datasource.DynamicDataSourceManager;
import com.metric.analyst.agent.service.datasource.DynamicQueryBuilder;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.*;
import java.util.*;

/**
 * 数据查询服务 - 基于 init_mysql.sql 表结构
 * 
 * 架构：
 * 1. db_data_source: 数据源配置（host/port/database/username/password）
 * 2. db_data_table: 每张指标物理表配置（table_name, time_column, region_column, value_column）
 * 3. db_data_dimension: 维度字段定义（dimension_id, dimension_code 映射到实际列）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DataQueryService {

    private final DataTableRepository dataTableRepository;
    private final DataSourceRepository dataSourceRepository;
    private final DataDimensionRepository dataDimensionRepository;
    private final DynamicDataSourceManager dataSourceManager;
    private final DynamicQueryBuilder queryBuilder;

    /**
     * 执行指标查询
     * 
     * @param tableId 表配置 ID（对应 db_data_table.table_id）
     * @param dimensions 标准化后的维度值
     * @return 查询结果
     */
    public QueryResult query(String tableId, 
                            DimensionNormalizationService.NormalizedDimensions dimensions) {
        
        try {
            // 1. 获取表配置
            DataTable dataTable = dataTableRepository.findByTableIdAndIsActive(tableId, true)
                .orElseThrow(() -> new IllegalArgumentException("数据表不存在或已禁用: " + tableId));
            
            // 2. 获取数据源配置
            DataSource dataSource = dataSourceRepository.findBySourceIdAndIsActive(dataTable.getSourceId(), true)
                .orElseThrow(() -> new IllegalArgumentException("数据源不存在或已禁用: " + dataTable.getSourceId()));
            
            // 3. 获取维度定义
            List<DataDimension> dimensionConfigs = dataDimensionRepository.findByTableId(tableId);
            
            // 4. 检查是否为排名查询
            DimensionNormalizationService.DimensionValue regionDim = dimensions.getDimensionValue("region");
            if (regionDim != null && isRegionLevelQuery(regionDim.getValueCode())) {
                return executeRankingQuery(dataTable, dataSource, dimensionConfigs, regionDim.getValueCode());
            }
            
            // 5. 构建并执行查询
            return executeStandardQuery(dataTable, dataSource, dimensionConfigs, dimensions);
            
        } catch (Exception e) {
            log.error("Query failed for table: {}", tableId, e);
            return QueryResult.error(e.getMessage());
        }
    }
    
    /**
     * 执行标准查询
     */
    private QueryResult executeStandardQuery(
            DataTable dataTable,
            DataSource dataSource,
            List<DataDimension> dimensionConfigs,
            DimensionNormalizationService.NormalizedDimensions dimensions) {
        
        // 构建 SQL
        DynamicQueryBuilder.SqlBuildResult sqlResult = queryBuilder.buildQuerySql(
            dataTable, dimensionConfigs, dimensions);
        
        log.debug("Executing SQL: {} with params: {}", sqlResult.getSql(), sqlResult.getParams());
        
        // 执行查询
        List<DataRow> rows = new ArrayList<>();
        try (Connection conn = dataSourceManager.getConnection(dataSource);
             PreparedStatement stmt = conn.prepareStatement(sqlResult.getSql())) {
            
            // 设置参数
            for (int i = 0; i < sqlResult.getParams().size(); i++) {
                stmt.setObject(i + 1, sqlResult.getParams().get(i));
            }
            
            // 执行查询
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    DataRow row = new DataRow();
                    row.setTimeId(rs.getString(dataTable.getTimeColumn()));
                    row.setRegionId(rs.getString(dataTable.getRegionColumn()));
                    row.setValue(rs.getBigDecimal(dataTable.getValueColumn()));
                    
                    // 获取其他维度值（使用 dimension_code）
                    Map<String, String> dimValues = new HashMap<>();
                    for (DataDimension dim : dimensionConfigs) {
                        if (dim.getDimensionCode() != null && !dim.getDimensionCode().isEmpty()) {
                            try {
                                String val = rs.getString(dim.getDimensionCode());
                                if (val != null) {
                                    dimValues.put(dim.getDimensionId(), val);
                                }
                            } catch (SQLException e) {
                                // 列可能不存在，忽略
                            }
                        }
                    }
                    row.setDimensionValues(dimValues);
                    
                    rows.add(row);
                }
            }
            
        } catch (SQLException e) {
            log.error("SQL execution failed", e);
            return QueryResult.error("查询执行失败: " + e.getMessage());
        }
        
        return processResults(rows, dataTable);
    }
    
    /**
     * 执行排名查询
     */
    private QueryResult executeRankingQuery(
            DataTable dataTable,
            DataSource dataSource,
            List<DataDimension> dimensionConfigs,
            String regionLevel) {
        
        // 构建排名 SQL
        DynamicQueryBuilder.SqlBuildResult sqlResult = queryBuilder.buildRankingSql(
            dataTable, dimensionConfigs, regionLevel, 20);
        
        log.debug("Executing ranking SQL: {}", sqlResult.getSql());
        
        List<RankingItem> ranking = new ArrayList<>();
        
        try (Connection conn = dataSourceManager.getConnection(dataSource);
             PreparedStatement stmt = conn.prepareStatement(sqlResult.getSql())) {
            
            // 设置参数
            for (int i = 0; i < sqlResult.getParams().size(); i++) {
                stmt.setObject(i + 1, sqlResult.getParams().get(i));
            }
            
            try (ResultSet rs = stmt.executeQuery()) {
                int rank = 1;
                while (rs.next()) {
                    ranking.add(new RankingItem(
                        rs.getString(dataTable.getRegionColumn()),
                        rs.getBigDecimal("total_value"),
                        rank++
                    ));
                }
            }
            
        } catch (SQLException e) {
            log.error("Ranking query failed", e);
            return QueryResult.error("排名查询失败: " + e.getMessage());
        }
        
        QueryResult result = new QueryResult();
        result.setSuccess(true);
        result.setRanking(ranking);
        result.setCount(ranking.size());
        
        if (!ranking.isEmpty()) {
            result.setLatestValue(ranking.get(0).getValue());
        }
        
        return result;
    }
    
    /**
     * 处理查询结果
     */
    private QueryResult processResults(List<DataRow> rows, DataTable dataTable) {
        if (rows.isEmpty()) {
            return QueryResult.empty();
        }
        
        QueryResult result = new QueryResult();
        result.setSuccess(true);
        result.setCount(rows.size());
        result.setRows(rows);
        
        // 按时间排序
        rows.sort((a, b) -> {
            if (a.getTimeId() == null || b.getTimeId() == null) return 0;
            return b.getTimeId().compareTo(a.getTimeId());
        });
        
        // 最新值
        DataRow latest = rows.get(0);
        result.setLatestValue(latest.getValue());
        
        // 计算排名（按值排序）
        List<DataRow> sortedByValue = new ArrayList<>(rows);
        sortedByValue.sort((a, b) -> {
            if (a.getValue() == null || b.getValue() == null) return 0;
            return b.getValue().compareTo(a.getValue());
        });
        
        List<RankingItem> ranking = new ArrayList<>();
        for (int i = 0; i < sortedByValue.size(); i++) {
            DataRow row = sortedByValue.get(i);
            ranking.add(new RankingItem(row.getRegionId(), row.getValue(), i + 1));
        }
        result.setRanking(ranking);
        
        // 计算趋势
        if (rows.size() > 1) {
            // 按时间正序排列
            List<DataRow> timeSorted = new ArrayList<>(rows);
            timeSorted.sort(Comparator.comparing(DataRow::getTimeId));
            
            DataRow first = timeSorted.get(0);
            DataRow last = timeSorted.get(timeSorted.size() - 1);
            
            BigDecimal startVal = first.getValue();
            BigDecimal endVal = last.getValue();
            
            if (startVal != null && endVal != null && startVal.compareTo(BigDecimal.ZERO) != 0) {
                double growth = endVal.subtract(startVal)
                    .divide(startVal, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .doubleValue();
                
                String trendType;
                int cmp = endVal.compareTo(startVal);
                if (cmp > 0) trendType = "上升";
                else if (cmp < 0) trendType = "下降";
                else trendType = "平稳";
                
                result.setTrend(TrendInfo.builder()
                    .startValue(startVal)
                    .endValue(endVal)
                    .growthRate(growth)
                    .trendType(trendType)
                    .build());
            }
        }
        
        return result;
    }
    
    /**
     * 判断是否为地区级别查询
     */
    private boolean isRegionLevelQuery(String regionCode) {
        return "省级".equals(regionCode) || "市级".equals(regionCode) || "全国".equals(regionCode);
    }
    
    // ============ DTO ============
    
    @Data
    public static class QueryResult {
        private boolean success;
        private String message;
        private int count;
        private BigDecimal latestValue;
        private List<DataRow> rows;
        private List<RankingItem> ranking;
        private TrendInfo trend;
        
        public static QueryResult empty() {
            QueryResult r = new QueryResult();
            r.success = true;
            r.message = "未查询到数据";
            r.rows = Collections.emptyList();
            r.ranking = Collections.emptyList();
            return r;
        }
        
        public static QueryResult error(String message) {
            QueryResult r = new QueryResult();
            r.success = false;
            r.message = message;
            return r;
        }
    }
    
    @Data
    public static class DataRow {
        private String timeId;
        private String regionId;
        private BigDecimal value;
        private Map<String, String> dimensionValues = new HashMap<>();
    }
    
    @Data
    @AllArgsConstructor
    public static class RankingItem {
        private String regionId;
        private BigDecimal value;
        private int rank;
    }
    
    @Data
    @Builder
    public static class TrendInfo {
        private BigDecimal startValue;
        private BigDecimal endValue;
        private double growthRate;
        private String trendType;
    }
}
