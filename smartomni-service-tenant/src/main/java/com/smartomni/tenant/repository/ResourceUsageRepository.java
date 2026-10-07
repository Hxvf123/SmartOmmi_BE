package com.smartomni.tenant.repository;

import com.smartomni.tenant.entity.ResourceUsage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

@org.springframework.transaction.annotation.Transactional(readOnly = true)
public interface ResourceUsageRepository extends JpaRepository<ResourceUsage, Long> {
    Optional<ResourceUsage> findByTenantId(Long tenantId);
}
