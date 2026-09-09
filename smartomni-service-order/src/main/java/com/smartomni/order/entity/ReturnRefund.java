package com.smartomni.order.entity;

import com.smartomni.common.entity.BaseTenantEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/** UC-13: Quan ly hoan/huy don hang tap trung da san. FR-030. */
@Getter
@Setter
@Entity
@Table(name = "returns_refunds")
public class ReturnRefund extends BaseTenantEntity {

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(columnDefinition = "TEXT")
    private String reason;

    @Enumerated(EnumType.STRING)
    private RefundStatus status = RefundStatus.REQUESTED;

    @Column(name = "restocked")
    private boolean restocked = false;

    public enum RefundStatus { REQUESTED, APPROVED, REJECTED, COMPLETED }
}
