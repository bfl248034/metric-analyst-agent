package com.metric.analyst.agent.repository;

import com.metric.analyst.agent.entity.DataSource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 数据源配置仓库
 */
@Repository
public interface DataSourceRepository extends JpaRepository<DataSource, Long> {

    Optional<DataSource> findBySourceId(String sourceId);

    Optional<DataSource> findBySourceIdAndIsActive(String sourceId, Boolean isActive);
}
