package com.smartomni.tenant.entity;

import com.smartomni.common.entity.BaseTenantEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/** UC-24: Xem canh bao gioi han tai nguyen. Theo doi muc dung so voi gioi han goi. */
@Getter
@Setter
@Entity
@Table(name = "resource_usage")
public class ResourceUsage extends BaseTenantEntity {

    @Column(name = "sku_count")
    private Integer skuCount = 0;

    @Column(name = "marketplace_connections_count")
    private Integer marketplaceConnectionsCount = 0;

    @Column(name = "storage_used_mb")
    private Integer storageUsedMb = 0;

    @Column(name = "last_calculated_at")
    private Instant lastCalculatedAt;
}
