package com.smartomni.inventory.entity;

import com.smartomni.common.entity.BaseTenantEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/**
 * UC-40: Dong bo ton kho nguoc lai san TMDT qua Outbox Pattern.
 * FR-074, FR-075, FR-076.
 * Ghi vao bang nay PHAI nam trong CUNG 1 transaction voi thao tac cap nhat Inventory
 * (dam bao "at-least-once delivery", khong bao gio mat su kien du co loi giua chung).
 */
@Getter
@Setter
@Entity
@Table(name = "inventory_outbox_events")
public class InventoryOutboxEvent extends BaseTenantEntity {

    @Column(name = "sku_id", nullable = false)
    private Long skuId;

    @Enumerated(EnumType.STRING)
    private Platform platform;

    private Integer quantity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OutboxStatus status = OutboxStatus.PENDING;

    @Column(name = "retry_count")
    private Integer retryCount = 0;

    @Column(name = "processed_at")
    private Instant processedAt;

    public enum Platform { SHOPEE, TIKTOK_SHOP }
    public enum OutboxStatus { PENDING, SENT, FAILED }
}
