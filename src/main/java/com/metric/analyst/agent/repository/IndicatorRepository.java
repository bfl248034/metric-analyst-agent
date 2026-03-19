package com.metric.analyst.agent.repository;

import com.metric.analyst.agent.entity.Indicator;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface IndicatorRepository extends JpaRepository<Indicator, Long> {

    Optional<Indicator> findByIndicatorId(String indicatorId);

    List<Indicator> findByDomain(String domain);

    @Query(value = "SELECT * FROM db_indicator WHERE MATCH(indicator_name, tags, remark) AGAINST(?1)", nativeQuery = true)
    List<Indicator> searchByFulltext(String keyword);

    List<Indicator> findByIndexedTrue();
}
