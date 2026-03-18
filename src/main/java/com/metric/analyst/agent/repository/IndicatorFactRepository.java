package com.metric.analyst.agent.repository;

import com.metric.analyst.agent.entity.IndicatorFact;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IndicatorFactRepository extends JpaRepository<IndicatorFact, Long> {

    @Query("SELECT f FROM IndicatorFact f WHERE f.indicatorId = :indicatorId " +
           "AND (:regionCode IS NULL OR f.regionCode = :regionCode) " +
           "AND (:timeId IS NULL OR f.timeId = :timeId) " +
           "ORDER BY f.timeId DESC")
    List<IndicatorFact> findByIndicatorAndRegion(@Param("indicatorId") String indicatorId,
                                                  @Param("regionCode") String regionCode,
                                                  @Param("timeId") String timeId);

    @Query("SELECT f FROM IndicatorFact f WHERE f.indicatorId = :indicatorId " +
           "AND f.regionCode = :regionCode ORDER BY f.timeId DESC")
    List<IndicatorFact> findTrendByIndicator(@Param("indicatorId") String indicatorId,
                                              @Param("regionCode") String regionCode);

    @Query("SELECT f FROM IndicatorFact f WHERE f.indicatorId = :indicatorId " +
           "AND f.timeId = :timeId ORDER BY f.value DESC")
    List<IndicatorFact> findRanking(@Param("indicatorId") String indicatorId,
                                     @Param("timeId") String timeId);
}
