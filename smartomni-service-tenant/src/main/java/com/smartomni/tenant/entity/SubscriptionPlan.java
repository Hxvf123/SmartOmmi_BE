package com.smartomni.tenant.entity;

import com.smartomni.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/** UC-29: Cau hinh/dinh nghia goi dich vu he thong (Super Admin). */
@Getter
@Setter
@Entity
@Table(name = "subscription_plans")
public class SubscriptionPlan extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String name; // Free, Pro...

    private BigDecimal price;

    @Column(name = "max_skus")
    private Integer maxSkus;

    @Column(name = "max_marketplace_connections")
    private Integer maxMarketplaceConnections;

    @Column(name = "storage_limit_mb")
    private Integer storageLimitMb;
}
