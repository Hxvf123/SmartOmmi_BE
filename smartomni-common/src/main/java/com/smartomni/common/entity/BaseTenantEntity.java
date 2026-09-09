package com.smartomni.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;

/**
 * Moi entity nghiep vu (Product, Order, Inventory...) BAT BUOC ke thua
 * class nay de dam bao luon co cot tenant_id - la co so de:
 *  - Ap dung Hibernate Filter theo tenant_id trong tung Repository
 *  - Ap dung PostgreSQL Row-Level Security (RLS) o tang Database
 * Xem UC-38 (Row-Level Security Enforcement) trong tai lieu UC/FR.
 */
@Getter
@Setter
@MappedSuperclass
public abstract class BaseTenantEntity extends BaseEntity {

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private Long tenantId;
}
