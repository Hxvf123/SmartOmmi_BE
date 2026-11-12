package com.smartomni.ai.config;

import com.smartomni.common.exception.FeatureNotAvailableException;
import com.smartomni.common.tenant.TenantContext;
import com.smartomni.common.tenant.TenantPlanResolver;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Kiểm soát quyền truy cập tính năng AI theo gói dịch vụ (chỉ mở cho gói PRO).
 */
@Component
public class AiSubscriptionGuard {

    public static final String AI_PRO_ONLY_MESSAGE = "Tính năng AI chỉ khả dụng cho gói Pro.";

    private final TenantPlanResolver tenantPlanResolver;

    public AiSubscriptionGuard(TenantPlanResolver tenantPlanResolver) {
        this.tenantPlanResolver = tenantPlanResolver;
    }

    @Autowired
    public AiSubscriptionGuard(ObjectProvider<TenantPlanResolver> tenantPlanResolverProvider) {
        this.tenantPlanResolver = tenantPlanResolverProvider != null
                ? tenantPlanResolverProvider.getIfAvailable()
                : null;
    }

    public void assertProPlanEnabled() {
        if (TenantContext.isSuperAdmin()) {
            return;
        }
        String plan = tenantPlanResolver != null
                ? tenantPlanResolver.resolveSubscriptionPlan(TenantContext.getTenantId())
                : TenantPlanResolver.normalizePlanName(TenantContext.getSubscriptionPlan());
        if (!"PRO".equalsIgnoreCase(plan)) {
            throw new FeatureNotAvailableException(AI_PRO_ONLY_MESSAGE);
        }
    }
}

