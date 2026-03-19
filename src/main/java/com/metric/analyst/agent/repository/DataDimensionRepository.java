package com.metric.analyst.agent.repository;

import com.metric.analyst.agent.entity.DataDimension;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DataDimensionRepository extends JpaRepository<DataDimension, Long> {

    List<DataDimension> findByTableId(String tableId);

    List<DataDimension> findByTableIdAndIsCommon(String tableId, Boolean isCommon);
}
