package com.smartomni.tenant.entity;

import com.smartomni.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/** Feature flag chi tiet theo tung goi (UC-29, UC-41 / FR-052,077-079). */
@Getter
@Setter
@Entity
@Table(name = "plan_feature_flags", uniqueConstraints = @UniqueConstraint(columnNames = {"plan_id", "feature_key"}))
public class PlanFeatureFlag extends BaseEntity {

    @Column(name = "plan_id", nullable = false)
    private Long planId;

    @Column(name = "feature_key", nullable = false)
    private String featureKey; // vd: demand_forecasting, recommendation_engine

    @Column(nullable = false)
    private boolean enabled = false;
}
