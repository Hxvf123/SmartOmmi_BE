package com.smartomni.tenant.entity;

import com.smartomni.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/**
 * UC-17 (Self-service Onboarding), UC-19 (Khoa/mo tai khoan Tenant).
 * Day la entity goc - KHONG ke thua BaseTenantEntity vi chinh no la Tenant.
 */
@Getter
@Setter
@Entity
@Table(name = "tenants")
public class Tenant extends BaseEntity {

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String subdomain;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TenantStatus status = TenantStatus.ACTIVE;

    @Column(name = "plan_id")
    private Long planId;

    @Column(name = "locked_reason")
    private String lockedReason;

    @Column(name = "locked_by")
    private Long lockedBy;

    @Column(name = "locked_at")
    private Instant lockedAt;

    public enum TenantStatus { ACTIVE, LOCKED }
}
