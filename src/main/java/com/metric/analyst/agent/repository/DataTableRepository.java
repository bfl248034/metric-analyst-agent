package com.metric.analyst.agent.repository;

import com.metric.analyst.agent.entity.DataTable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 数据表配置仓库
 */
@Repository
public interface DataTableRepository extends JpaRepository<DataTable, String> {

    Optional<DataTable> findByTableId(String tableId);

    List<DataTable> findBySourceId(String sourceId);

    Optional<DataTable> findByTableIdAndIsActive(String tableId, Boolean isActive);
}
