package com.metric.analyst.agent.repository;

import com.metric.analyst.agent.entity.DimensionValue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DimensionValueRepository extends JpaRepository<DimensionValue, Long> {

    List<DimensionValue> findByDimensionId(String dimensionId);

    Optional<DimensionValue> findByDimensionIdAndValueCode(String dimensionId, String valueCode);

    List<DimensionValue> findByParentCode(String parentCode);

    List<DimensionValue> findByIndexedTrue();
}
