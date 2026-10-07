package com.smartomni.order.entity;

import org.hibernate.annotations.JdbcType;
import com.smartomni.common.persistence.LowercasePostgreSQLEnumJdbcType;
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

    @Enumerated(EnumType.STRING)
    @JdbcType(LowercasePostgreSQLEnumJdbcType.class)
    @Column(name = "trigger_type", nullable = false, columnDefinition = "return_trigger_type")
    private TriggerType triggerType = TriggerType.CUSTOMER_REQUEST;

    @Column(columnDefinition = "TEXT")
    private String reason;

    @Enumerated(EnumType.STRING)
    @JdbcType(LowercasePostgreSQLEnumJdbcType.class)
    @Column(columnDefinition = "refund_status")
    private RefundStatus status = RefundStatus.REQUESTED;

    @Column(name = "restocked")
    private boolean restocked = false;

    public enum TriggerType { REFUSED_DELIVERY, CUSTOMER_REQUEST }

    public enum RefundStatus { REQUESTED, APPROVED, REJECTED, COMPLETED }
}
