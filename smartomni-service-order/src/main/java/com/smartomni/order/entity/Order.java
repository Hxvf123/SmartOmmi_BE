package com.smartomni.order.entity;

import com.smartomni.common.entity.BaseTenantEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * UC-10: Theo doi & xu ly don hang.
 * Unique constraint (tenant_id, platform, platform_order_id) chinh la co che
 * chong trung lap (idempotency) khi Webhook va Polling cung ghi nhan 1 don (FR-025).
 */
@Getter
@Setter
@Entity
@Table(name = "orders", uniqueConstraints = @UniqueConstraint(columnNames = {"tenant_id", "platform", "platform_order_id"}))
public class Order extends BaseTenantEntity {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Platform platform;

    @Column(name = "platform_order_id")
    private String platformOrderId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status = OrderStatus.PENDING;

    @Column(name = "customer_name")
    private String customerName;

    @Column(name = "customer_phone")
    private String customerPhone;

    @Column(name = "shipping_address", columnDefinition = "TEXT")
    private String shippingAddress;

    @Column(name = "total_amount")
    private BigDecimal totalAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderSource source; // WEBHOOK | POLLING | MANUAL - FR-025

    public enum Platform { SHOPEE, TIKTOK_SHOP }
    public enum OrderStatus { PENDING, CONFIRMED, SHIPPING, DELIVERED, CANCELLED, RETURNED }
    public enum OrderSource { WEBHOOK, POLLING, MANUAL }
}
