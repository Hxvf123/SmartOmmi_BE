package com.smartomni.tenant.repository;

import com.smartomni.tenant.entity.PlanFeatureFlag;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PlanFeatureFlagRepository extends JpaRepository<PlanFeatureFlag, Long> {
    List<PlanFeatureFlag> findByPlanId(Long planId);
    Optional<PlanFeatureFlag> findByPlanIdAndFeatureKey(Long planId, String featureKey);
}
