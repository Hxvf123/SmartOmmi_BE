package com.smartomni.tenant.controller;

import com.smartomni.common.dto.ApiResponse;
import com.smartomni.tenant.dto.LockTenantRequest;
import com.smartomni.tenant.dto.TenantOnboardingRequest;
import com.smartomni.tenant.entity.Tenant;
import com.smartomni.tenant.service.TenantService;
import com.smartomni.common.tenant.TenantContext;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

/**
 * UC-17 (Onboarding), UC-25 (Danh sach Tenant), UC-19 (Khoa/mo Tenant).
 * Cac endpoint /lock, /unlock, danh sach toan bo Tenant chi danh cho SUPER_ADMIN
 * (kiem tra role qua TenantContext.isSuperAdmin() hoac @PreAuthorize khi tich hop Spring Security day du).
 */
@RestController
@RequestMapping("/api/tenants")
@RequiredArgsConstructor
public class TenantController {

    private final TenantService tenantService;

    @PostMapping("/onboarding")
    public ApiResponse<Tenant> onboard(@Valid @RequestBody TenantOnboardingRequest request) {
        return ApiResponse.success("Khoi tao Tenant thanh cong", tenantService.onboardTenant(request));
    }

    @GetMapping
    public ApiResponse<Page<Tenant>> listTenants(@RequestParam(required = false) String keyword, Pageable pageable) {
        // TODO: chan neu !TenantContext.isSuperAdmin()
        return ApiResponse.success(tenantService.searchTenants(keyword, pageable));
    }

    @PostMapping("/{tenantId}/lock")
    public ApiResponse<Void> lock(@PathVariable Long tenantId, @Valid @RequestBody LockTenantRequest request) {
        tenantService.lockTenant(tenantId, request.getReason(), TenantContext.getCurrentUserId());
        return ApiResponse.success("Da khoa Tenant", null);
    }

    @PostMapping("/{tenantId}/unlock")
    public ApiResponse<Void> unlock(@PathVariable Long tenantId) {
        tenantService.unlockTenant(tenantId);
        return ApiResponse.success("Da mo khoa Tenant", null);
    }

    @GetMapping("/{tenantId}/status")
    public ApiResponse<Boolean> isActive(@PathVariable Long tenantId) {
        return ApiResponse.success(tenantService.isTenantActive(tenantId));
    }
}
