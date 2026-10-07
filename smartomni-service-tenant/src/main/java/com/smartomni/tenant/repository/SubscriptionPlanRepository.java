package com.smartomni.tenant.repository;

import com.smartomni.tenant.entity.SubscriptionPlan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

@org.springframework.transaction.annotation.Transactional(readOnly = true)
public interface SubscriptionPlanRepository extends JpaRepository<SubscriptionPlan, Long> {
    Optional<SubscriptionPlan> findByName(String name);
}
