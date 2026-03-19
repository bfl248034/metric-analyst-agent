package com.metric.analyst.agent.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * 指标元数据表 - db_indicator
 */
@Data
@Entity
@Table(name = "db_indicator")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Indicator {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "indicator_id", unique = true, nullable = false, length = 64)
    private String indicatorId;

    @Column(name = "indicator_name", nullable = false, length = 128)
    private String indicatorName;

    @Column(name = "unit", length = 32)
    private String unit;

    @Column(name = "frequency", nullable = false, length = 10)
    private String frequency;

    @Column(name = "valid_measures", length = 256)
    private String validMeasures;

    @Column(name = "table_id", length = 128)
    private String tableId;

    @Column(name = "remark", columnDefinition = "TEXT")
    private String remark;

    @Column(name = "domain", length = 64)
    private String domain;

    @Column(name = "subdomain", length = 64)
    private String subdomain;

    @Column(name = "tags", length = 256)
    private String tags;

    @Column(name = "indexed")
    private Boolean indexed;

    @Column(name = "index_version")
    private Long indexVersion;

    @Column(name = "last_indexed_at")
    private LocalDateTime lastIndexedAt;

    @Column(name = "embedding_json", columnDefinition = "TEXT")
    private String embeddingJson;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
