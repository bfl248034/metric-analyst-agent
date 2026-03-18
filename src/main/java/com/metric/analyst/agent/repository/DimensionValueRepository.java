package com.metric.analyst.agent.repository;

import com.metric.analyst.agent.entity.DimensionValue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DimensionValueRepository extends JpaRepository<DimensionValue, Long> {
    
    List<DimensionValue> findByDimensionId(String dimensionId);
    
    DimensionValue findByDimensionIdAndValueCode(String dimensionId, String valueCode);
}
