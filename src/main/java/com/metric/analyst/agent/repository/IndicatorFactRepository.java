package com.metric.analyst.agent.repository;

import com.metric.analyst.agent.entity.IndicatorFact;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IndicatorFactRepository extends JpaRepository<IndicatorFact, Long> {

    /**
     * 根据指标编码和地区编码查询，按年份月份降序
     */
    @Query("SELECT f FROM IndicatorFact f WHERE f.indicatorCode = :indicatorCode " +
           "AND f.regionCode = :regionCode " +
           "ORDER BY f.year DESC, f.month DESC")
    List<IndicatorFact> findByIndicatorCodeAndRegionCodeOrderByYearDescMonthDesc(
            @Param("indicatorCode") String indicatorCode,
            @Param("regionCode") String regionCode);

    /**
     * 根据指标编码查询所有地区数据
     */
    @Query("SELECT f FROM IndicatorFact f WHERE f.indicatorCode = :indicatorCode " +
           "ORDER BY f.year DESC, f.month DESC")
    List<IndicatorFact> findByIndicatorCodeOrderByYearDescMonthDesc(
            @Param("indicatorCode") String indicatorCode);

    /**
     * 查询特定年份月份的数据
     */
    @Query("SELECT f FROM IndicatorFact f WHERE f.indicatorCode = :indicatorCode " +
           "AND f.year = :year AND f.month = :month " +
           "ORDER BY f.metricValue DESC")
    List<IndicatorFact> findByIndicatorCodeAndYearAndMonthOrderByMetricValueDesc(
            @Param("indicatorCode") String indicatorCode,
            @Param("year") Integer year,
            @Param("month") Integer month);
}
