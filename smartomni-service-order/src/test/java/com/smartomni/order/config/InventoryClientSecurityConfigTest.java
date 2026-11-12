package com.smartomni.order.config;

import com.smartomni.common.security.JwtTokenProvider;
import com.smartomni.common.tenant.TenantContext;
import feign.RequestTemplate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class InventoryClientSecurityConfigTest {
    private final JwtTokenProvider tokens = mock(JwtTokenProvider.class);
    private final InventoryClientSecurityConfig config = new InventoryClientSecurityConfig();

    @AfterEach void clear() { TenantContext.clear(); }

    @Test void signsVerifiedTenantForInventoryCall() {
        TenantContext.setTenantId(7L);
        when(tokens.generateToken(0L, "INTERNAL_SERVICE", 7L)).thenReturn("signed");
        RequestTemplate request = new RequestTemplate();
        config.inventoryRequestInterceptor(tokens).apply(request);
        assertTrue(request.headers().get("Authorization").contains("Bearer signed"));
    }

    @Test void refusesCallWithoutTenant() {
        assertThrows(IllegalStateException.class,
                () -> config.inventoryRequestInterceptor(tokens).apply(new RequestTemplate()));
        verifyNoInteractions(tokens);
    }
}
