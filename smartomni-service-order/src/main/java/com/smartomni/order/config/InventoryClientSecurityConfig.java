package com.smartomni.order.config;

import com.smartomni.common.security.JwtTokenProvider;
import com.smartomni.common.tenant.TenantContext;
import feign.RequestInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class InventoryClientSecurityConfig {
    @Bean
    public RequestInterceptor inventoryRequestInterceptor(JwtTokenProvider tokens) {
        return request -> {
            Long tenantId = TenantContext.getTenantId();
            if (tenantId == null) {
                throw new IllegalStateException("Inventory call requires verified tenant context");
            }
            request.header("Authorization", "Bearer " + tokens.generateToken(0L, "INTERNAL_SERVICE", tenantId));
        };
    }
}
