package com.smartomni.tenant.service;

import com.smartomni.common.exception.FeatureNotAvailableException;
import com.smartomni.tenant.entity.PlanFeatureFlag;
import com.smartomni.tenant.entity.ResourceUsage;
import com.smartomni.tenant.entity.Tenant;
import com.smartomni.tenant.repository.PlanFeatureFlagRepository;
import com.smartomni.tenant.repository.ResourceUsageRepository;
import com.smartomni.tenant.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

/**
 * UC-41: Thuc thi gioi han tai nguyen theo goi dich vu (Feature Flag Enforcement at Runtime).
 * FR-077, FR-078, FR-079.
 *
 * Cac service khac (Catalog, AI...) goi sang service nay (qua REST/Feign) truoc khi
 * thuc hien hanh dong can kiem tra quyen (vd tao SKU moi, bat Demand Forecasting).
 * Ket qua duoc cache (Spring Cache + Redis) va PHAI duoc @CacheEvict ngay khi Tenant
 * doi goi dich vu (UC-21) de tranh chan nham (FR-079).
 */
@Service
@RequiredArgsConstructor
public class FeatureFlagService {

    private final TenantRepository tenantRepository;
    private final PlanFeatureFlagRepository featureFlagRepository;
    private final ResourceUsageRepository resourceUsageRepository;

    @Cacheable(value = "featureFlags", key = "#tenantId + ':' + #featureKey")
    public boolean isFeatureEnabled(Long tenantId, String featureKey) {
        Long planId = tenantRepository.findById(tenantId)
                .map(Tenant::getPlanId)
                .orElse(null);
        if (planId == null) return false;

        return featureFlagRepository.findByPlanIdAndFeatureKey(planId, featureKey)
                .map(PlanFeatureFlag::isEnabled)
                .orElse(false);
    }

    public void assertFeatureEnabled(Long tenantId, String featureKey) {
        if (!isFeatureEnabled(tenantId, featureKey)) {
            throw new FeatureNotAvailableException(
                    "Tinh nang '" + featureKey + "' khong kha dung trong goi dich vu hien tai cua Tenant");
        }
    }

    /** FR-020, FR-086: kiem tra gioi han SKU truoc khi tao san pham/nhap tu dong. */
    public void assertSkuLimitNotExceeded(Long tenantId, int additionalSkus) {
        Tenant tenant = tenantRepository.findById(tenantId).orElseThrow();
        // TODO: lay maxSkus tu SubscriptionPlan cua tenant.getPlanId()
        ResourceUsage usage = resourceUsageRepository.findByTenantId(tenantId)
                .orElseGet(() -> {
                    ResourceUsage ru = new ResourceUsage();
                    ru.setTenantId(tenantId);
                    return ru;
                });
        int projected = (usage.getSkuCount() == null ? 0 : usage.getSkuCount()) + additionalSkus;
        // int maxSkus = ... (tra cuu tu SubscriptionPlan)
        // if (projected > maxSkus) throw new FeatureNotAvailableException("Vuot gioi han SKU cua goi dich vu");
    }

    @CacheEvict(value = "featureFlags", allEntries = true)
    public void evictFeatureFlagCache() {
        // Goi ham nay ngay sau khi Tenant doi goi dich vu (UC-21) - FR-079
    }
}
