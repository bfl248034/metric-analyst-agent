package com.metric.analyst.agent.service;

import com.metric.analyst.agent.entity.DimensionValue;
import com.metric.analyst.agent.entity.IndicatorFact;
import com.metric.analyst.agent.repository.IndicatorFactRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 数据查询服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DataQueryService {

    private final JdbcTemplate jdbcTemplate;
    private final DimensionNormalizationService dimensionService;

    /**
     * 执行指标查询
     */
    public QueryResult query(String tableId, 
                            DimensionNormalizationService.NormalizedDimensions dimensions) {
        try {
            // 1. 展开时间（如果是近N期）
            DimensionValue timeDim = dimensions.getDimensionValue("time");
            List<String> timeList = expandTimeIfNeeded(tableId, timeDim);

            // 2. 构建SQL
            String sql = buildSql(tableId, dimensions, timeList);
            log.debug("Executing SQL: {}", sql);

            // 3. 执行查询
            List<IndicatorFact> facts = jdbcTemplate.query(sql, new IndicatorFactRowMapper(tableId));

            // 4. 后处理
            return processResults(facts, dimensions);

        } catch (Exception e) {
            log.error("Query failed", e);
            return QueryResult.error(e.getMessage());
        }
    }

    /**
     * 展开时间（近N期）
     */
    private List<String> expandTimeIfNeeded(String tableId, DimensionValue timeDim) {
        if (timeDim == null || timeDim.getValueCode() == null) {
            return Collections.emptyList();
        }

        String timeCode = timeDim.getValueCode();

        if ("latest".equals(timeCode)) {
            // 查询最新时间
            String sql = String.format("SELECT MAX(time_id) FROM %s", tableId);
            String latest = jdbcTemplate.queryForObject(sql, String.class);
            return latest != null ? List.of(latest) : Collections.emptyList();
        }

        if (timeCode.startsWith("last:")) {
            // 展开近N期
            return dimensionService.expandTimePeriods(tableId, timeCode);
        }

        return List.of(timeCode);
    }

    /**
     * 构建SQL
     */
    private String buildSql(String tableId, 
                           DimensionNormalizationService.NormalizedDimensions dimensions,
                           List<String> timeList) {
        StringBuilder sql = new StringBuilder("SELECT * FROM ").append(tableId).append(" WHERE 1=1");

        // 时间条件
        if (timeList.size() == 1) {
            sql.append(" AND time_id = '").append(timeList.get(0)).append("'");
        } else if (timeList.size() > 1) {
            String times = timeList.stream()
                .map(t -> "'" + t + "'")
                .collect(Collectors.joining(","));
            sql.append(" AND time_id IN (").append(times).append(")");
        }

        // 地区条件（特殊处理：级别查询 vs 具体编码）
        DimensionValue regionDim = dimensions.getDimensionValue("region");
        if (regionDim != null) {
            String regionCode = regionDim.getValueCode();
            if ("省级".equals(regionCode) || "市级".equals(regionCode) || "全国".equals(regionCode)) {
                // 级别查询
                sql.append(" AND region_level = '").append(regionCode).append("'");
            } else {
                // 具体编码
                sql.append(" AND region_id = '").append(regionCode).append("'");
            }
        }

        // 其他维度条件
        Map<String, DimensionValue> allDims = new HashMap<>();
        allDims.putAll(dimensions.getExplicitDimensions());
        allDims.putAll(dimensions.getImplicitDimensions());

        for (Map.Entry<String, DimensionValue> entry : allDims.entrySet()) {
            String dimId = entry.getKey();
            DimensionValue dimValue = entry.getValue();

            if ("time".equals(dimId) || "region".equals(dimId)) {
                continue;  // 已处理
            }

            String columnName = dimId + "_id";
            String valueCode = dimValue.getValueCode();

            // 判断是否多值查询（排除TOTAL）
            if (dimensionService.isMultiValueQuery(dimValue.getValueName())) {
                sql.append(" AND ").append(columnName).append(" != 'TOTAL'");
            } else {
                sql.append(" AND ").append(columnName).append(" = '").append(valueCode).append("'");
            }
        }

        sql.append(" ORDER BY time_id, region_id");
        return sql.toString();
    }

    /**
     * 处理查询结果
     */
    private QueryResult processResults(List<IndicatorFact> facts, 
                                      DimensionNormalizationService.NormalizedDimensions dimensions) {
        if (facts.isEmpty()) {
            return QueryResult.empty();
        }

        QueryResult result = new QueryResult();
        result.setFacts(facts);
        result.setCount(facts.size());

        // 计算排名
        List<QueryResult.RankingItem> ranking = facts.stream()
            .sorted(Comparator.comparing(IndicatorFact::getFactValue).reversed())
            .map(f -> new QueryResult.RankingItem(
                f.getRegionId(),
                f.getFactValue(),
                0  // rank will be set
            ))
            .collect(Collectors.toList());
        
        // 设置排名
        for (int i = 0; i < ranking.size(); i++) {
            ranking.get(i).setRank(i + 1);
        }
        result.setRanking(ranking);

        // 计算趋势（如果有时间序列）
        if (facts.size() > 1) {
            List<IndicatorFact> sortedByTime = facts.stream()
                .sorted(Comparator.comparing(IndicatorFact::getTimeId))
                .toList();
            
            IndicatorFact first = sortedByTime.get(0);
            IndicatorFact last = sortedByTime.get(sortedByTime.size() - 1);
            
            result.setTrend(QueryResult.TrendInfo.builder()
                .startValue(first.getFactValue())
                .endValue(last.getFactValue())
                .growthRate(calculateGrowth(first.getFactValue(), last.getFactValue()))
                .trendType(determineTrend(first.getFactValue(), last.getFactValue()))
                .build());
        }

        // 最新值
        IndicatorFact latest = facts.stream()
            .max(Comparator.comparing(IndicatorFact::getTimeId))
            .orElse(facts.get(0));
        result.setLatestValue(latest.getFactValue());
        result.setLatestMom(latest.getValueMom());
        result.setLatestYoy(latest.getValueYoy());

        return result;
    }

    private double calculateGrowth(BigDecimal start, BigDecimal end) {
        if (start == null || end == null || start.compareTo(BigDecimal.ZERO) == 0) {
            return 0;
        }
        return end.subtract(start).divide(start, 4, BigDecimal.ROUND_HALF_UP)
            .multiply(BigDecimal.valueOf(100)).doubleValue();
    }

    private String determineTrend(BigDecimal start, BigDecimal end) {
        if (start == null || end == null) return "平稳";
        int cmp = end.compareTo(start);
        if (cmp > 0) return "上升";
        if (cmp < 0) return "下降";
        return "平稳";
    }

    // RowMapper
    private static class IndicatorFactRowMapper implements RowMapper<IndicatorFact> {
        private final String tableId;

        public IndicatorFactRowMapper(String tableId) {
            this.tableId = tableId;
        }

        @Override
        public IndicatorFact mapRow(ResultSet rs, int rowNum) throws SQLException {
            return IndicatorFact.builder()
                .tableId(tableId)
                .timeId(rs.getDate("time_id").toLocalDate())
                .regionId(rs.getString("region_id"))
                .regionLevel(rs.getString("region_level"))
                .educationId(rs.getString("education_id"))
                .economicTypeId(rs.getString("economic_type_id"))
                .factValue(rs.getBigDecimal("fact_value"))
                .valueMom(rs.getBigDecimal("value_mom"))
                .valueYoy(rs.getBigDecimal("value_yoy"))
                .build();
        }
    }

    // DTO
    @Data
    public static class QueryResult {
        private boolean success;
        private String message;
        private List<IndicatorFact> facts;
        private int count;
        private BigDecimal latestValue;
        private BigDecimal latestMom;
        private BigDecimal latestYoy;
        private List<RankingItem> ranking;
        private TrendInfo trend;

        public static QueryResult empty() {
            QueryResult r = new QueryResult();
            r.success = true;
            r.message = "未查询到数据";
            r.facts = Collections.emptyList();
            return r;
        }

        public static QueryResult error(String message) {
            QueryResult r = new QueryResult();
            r.success = false;
            r.message = message;
            return r;
        }

        @Data
        public static class RankingItem {
            private final String regionId;
            private final BigDecimal value;
            private int rank;
        }

        @Data
        @lombok.Builder
        public static class TrendInfo {
            private BigDecimal startValue;
            private BigDecimal endValue;
            private double growthRate;
            private String trendType;  // 上升/下降/平稳
        }
    }
}
