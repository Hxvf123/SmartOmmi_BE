package com.smartomni.ai.repository;

import com.smartomni.ai.entity.DemandForecast;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DemandForecastRepository extends JpaRepository<DemandForecast, Long> {
    List<DemandForecast> findByTenantIdAndSkuId(Long tenantId, Long skuId);
}
