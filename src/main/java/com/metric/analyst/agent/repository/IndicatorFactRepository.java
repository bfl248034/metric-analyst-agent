package com.metric.analyst.agent.repository;

import com.metric.analyst.agent.entity.IndicatorFact;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IndicatorFactRepository extends JpaRepository<IndicatorFact, Long> {

    @Query(value = "SELECT MAX(time_id) FROM :tableId", nativeQuery = true)
    String findMaxTimeIdByTableId(@Param("tableId") String tableId);

    List<IndicatorFact> findByTableIdAndTimeId(String tableId, String timeId);

    List<IndicatorFact> findByTableIdAndRegionId(String tableId, String regionId);
}
