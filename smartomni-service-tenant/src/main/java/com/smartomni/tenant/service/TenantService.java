package com.smartomni.tenant.service;

import com.smartomni.common.exception.BusinessException;
import com.smartomni.common.exception.ResourceNotFoundException;
import com.smartomni.tenant.dto.TenantOnboardingRequest;
import com.smartomni.tenant.entity.SubscriptionPlan;
import com.smartomni.tenant.entity.Tenant;
import com.smartomni.tenant.repository.SubscriptionPlanRepository;
import com.smartomni.tenant.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * UC-17: Dang ky & khoi tao Tenant (Self-service Onboarding) - FR-035,036,037
 * UC-25: Quan ly danh sach Tenant - FR-046,047
 * UC-19: Khoa/mo tai khoan Tenant - FR-048,049
 */
@Service
@RequiredArgsConstructor
public class TenantService {

    private final TenantRepository tenantRepository;
    private final SubscriptionPlanRepository subscriptionPlanRepository;
    // private final AuthServiceClient authServiceClient; // TODO: Feign client goi sang smartomni-service-auth de tao Admin user

    @Transactional
    public Tenant onboardTenant(TenantOnboardingRequest request) {
        // FR-036: kiem tra subdomain duy nhat
        if (tenantRepository.existsBySubdomain(request.getSubdomain())) {
            throw new BusinessException("Subdomain '" + request.getSubdomain() + "' da ton tai, vui long chon ten khac");
        }

        SubscriptionPlan plan = subscriptionPlanRepository.findByName(request.getInitialPlanName())
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay goi dich vu: " + request.getInitialPlanName()));

        Tenant tenant = new Tenant();
        tenant.setName(request.getBusinessName());
        tenant.setSubdomain(request.getSubdomain());
        tenant.setPlanId(plan.getId());
        tenant.setStatus(Tenant.TenantStatus.ACTIVE);
        tenant = tenantRepository.save(tenant); // FR-037: tenant_id (id) tu sinh, dung lam khoa cho moi du lieu sau nay

        // TODO: goi smartomni-service-auth de tao tai khoan Admin dau tien cho Tenant nay
        // authServiceClient.createAdminUser(tenant.getId(), request.getAdminEmail(), request.getAdminPassword());

        return tenant;
    }

    public Page<Tenant> searchTenants(String keyword, Pageable pageable) {
        String kw = keyword == null ? "" : keyword;
        return tenantRepository.findByNameContainingIgnoreCaseOrSubdomainContainingIgnoreCase(kw, kw, pageable);
    }

    @Transactional
    public void lockTenant(Long tenantId, String reason, Long lockedByUserId) {
        Tenant tenant = getTenantOrThrow(tenantId);
        tenant.setStatus(Tenant.TenantStatus.LOCKED);
        tenant.setLockedReason(reason);
        tenant.setLockedBy(lockedByUserId);
        tenant.setLockedAt(Instant.now());
        tenantRepository.save(tenant);
    }

    @Transactional
    public void unlockTenant(Long tenantId) {
        Tenant tenant = getTenantOrThrow(tenantId);
        tenant.setStatus(Tenant.TenantStatus.ACTIVE);
        tenant.setLockedReason(null);
        tenant.setLockedBy(null);
        tenant.setLockedAt(null);
        tenantRepository.save(tenant);
    }

    public Tenant getTenantOrThrow(Long tenantId) {
        return tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant khong ton tai: " + tenantId));
    }

    /** Duoc goi boi Gateway/cac service khac de kiem tra nhanh Tenant co dang active khong (UC-03/FR-003). */
    public boolean isTenantActive(Long tenantId) {
        return tenantRepository.findById(tenantId)
                .map(t -> t.getStatus() == Tenant.TenantStatus.ACTIVE)
                .orElse(false);
    }
}
