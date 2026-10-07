package com.smartomni.catalog.entity;

import org.hibernate.annotations.JdbcType;
import com.smartomni.common.persistence.LowercasePostgreSQLEnumJdbcType;
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
    @JdbcType(LowercasePostgreSQLEnumJdbcType.class)
    @Column(nullable = false, columnDefinition = "sync_scope")
    private SyncScope scope; // TENANT hoac PRODUCT

    @Column(name = "sku_id")
    private Long skuId; // null neu scope = TENANT

    @Enumerated(EnumType.STRING)
    @JdbcType(LowercasePostgreSQLEnumJdbcType.class)
    @Column(columnDefinition = "platform_type")
    private ProductMarketplaceLink.Platform platform;

    @Column(name = "sync_enabled")
    private boolean syncEnabled = false;

    @Column(name = "sync_frequency_minutes")
    private Integer syncFrequencyMinutes;

    public enum SyncScope { TENANT, PRODUCT }
}
