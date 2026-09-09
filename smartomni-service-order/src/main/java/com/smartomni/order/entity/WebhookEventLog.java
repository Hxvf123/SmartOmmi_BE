package com.smartomni.order.entity;

import com.smartomni.common.entity.BaseTenantEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/** UC-32: Dong bo don hang qua Webhook - log de audit va debug. FR-058,059,060. */
@Getter
@Setter
@Entity
@Table(name = "webhook_events_log")
public class WebhookEventLog extends BaseTenantEntity {

    @Enumerated(EnumType.STRING)
    private Order.Platform platform;

    @Column(name = "signature_valid")
    private boolean signatureValid;

    @Column(name = "raw_payload", columnDefinition = "TEXT")
    private String rawPayload;

    @Column(name = "queued")
    private boolean queued = false;
}
