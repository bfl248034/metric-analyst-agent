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
 * 维度值表 - dimension_values
 */
@Data
@Entity
@Table(name = "dimension_values")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DimensionValue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "dimension_id", nullable = false, length = 64)
    private String dimensionId;

    @Column(name = "dimension_name", length = 64)
    private String dimensionName;

    @Column(name = "value_code", nullable = false, length = 64)
    private String valueCode;

    @Column(name = "value_name", nullable = false, length = 128)
    private String valueName;

    @Column(name = "synonyms", length = 500)
    private String synonyms;

    @Column(name = "parent_code", length = 64)
    private String parentCode;

    @Column(name = "sort_order")
    private Integer sortOrder;

    @Column(name = "indexed")
    private Boolean indexed;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
