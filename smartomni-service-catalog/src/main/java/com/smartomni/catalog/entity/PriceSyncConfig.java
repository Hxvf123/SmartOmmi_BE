package com.smartomni.catalog.entity;

import com.smartomni.common.entity.BaseTenantEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/** UC-23: Bat/tat tuy chon tu dong dong bo gia tu san TMDT. */
@Getter
@Setter
@Entity
@Table(name = "price_sync_configs")
public class PriceSyncConfig extends BaseTenantEntity {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SyncScope scope; // TENANT hoac PRODUCT

    @Column(name = "sku_id")
    private Long skuId; // null neu scope = TENANT

    @Enumerated(EnumType.STRING)
    private ProductMarketplaceLink.Platform platform;

    @Column(name = "sync_enabled")
    private boolean syncEnabled = false;

    @Column(name = "sync_frequency_minutes")
    private Integer syncFrequencyMinutes;

    public enum SyncScope { TENANT, PRODUCT }
}
