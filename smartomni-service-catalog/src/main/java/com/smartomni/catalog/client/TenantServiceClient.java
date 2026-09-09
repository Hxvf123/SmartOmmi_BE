package com.smartomni.catalog.client;

import com.smartomni.common.dto.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Goi sang smartomni-service-tenant de kiem tra feature flag / gioi han SKU
 * truoc khi Manager tao san pham moi (UC-08, UC-20/FR-020, UC-41).
 * TODO: cau hinh Eureka/service discovery hoac Spring Cloud LoadBalancer
 * de thay the hard-code url ben duoi bang service-name (vd http://smartomni-service-tenant).
 */
@FeignClient(name = "smartomni-service-tenant", url = "${smartomni.services.tenant-url:http://localhost:8082}")
public interface TenantServiceClient {

    @GetMapping("/api/tenants/{tenantId}/status")
    ApiResponse<Boolean> isTenantActive(@PathVariable("tenantId") Long tenantId);
}
