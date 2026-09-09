package com.smartomni.catalog.repository;

import com.smartomni.catalog.entity.PriceSyncConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PriceSyncConfigRepository extends JpaRepository<PriceSyncConfig, Long> {
    List<PriceSyncConfig> findByTenantIdAndSyncEnabledTrue(Long tenantId);
    List<PriceSyncConfig> findBySyncEnabledTrue(); // dung cho scheduler quet toan he thong
}
