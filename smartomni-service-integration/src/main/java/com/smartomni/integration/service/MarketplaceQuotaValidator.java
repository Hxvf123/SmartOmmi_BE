package com.smartomni.integration.service;

import com.smartomni.common.exception.FeatureNotAvailableException;
import com.smartomni.common.tenant.TenantContext;
import com.smartomni.common.tenant.TenantPlanResolver;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Kiểm tra giới hạn số lượng sàn TMĐT theo gói cước (Free tối đa 1 sàn, Pro không giới hạn).
 */
@Component
public class MarketplaceQuotaValidator {

    public static final int FREE_MAX_SHOPS = 1;
    public static final String FREE_LIMIT_MESSAGE = "Gói Free chỉ hỗ trợ kết nối tối đa 1 sàn TMĐT.";

    private final TenantPlanResolver tenantPlanResolver;

    public MarketplaceQuotaValidator(TenantPlanResolver tenantPlanResolver) {
        this.tenantPlanResolver = tenantPlanResolver;
    }

    @Autowired
    public MarketplaceQuotaValidator(ObjectProvider<TenantPlanResolver> tenantPlanResolverProvider) {
        this.tenantPlanResolver = tenantPlanResolverProvider != null
                ? tenantPlanResolverProvider.getIfAvailable()
                : null;
    }

    public void assertCanConnect(Long tenantId, long currentActiveConnections) {
        if (TenantContext.isSuperAdmin()) {
            return;
        }
        String plan = tenantPlanResolver != null
                ? tenantPlanResolver.resolveSubscriptionPlan(tenantId)
                : TenantPlanResolver.normalizePlanName(TenantContext.getSubscriptionPlan());
        if ("FREE".equalsIgnoreCase(plan) && currentActiveConnections >= FREE_MAX_SHOPS) {
            throw new FeatureNotAvailableException(FREE_LIMIT_MESSAGE);
        }
    }
}

