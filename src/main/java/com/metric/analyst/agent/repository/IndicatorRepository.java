package com.metric.analyst.agent.repository;

import com.metric.analyst.agent.entity.Indicator;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface IndicatorRepository extends JpaRepository<Indicator, String> {
    
    Optional<Indicator> findByIndicatorId(String indicatorId);
}
