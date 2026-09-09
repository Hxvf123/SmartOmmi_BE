package com.smartomni.tenant.entity;

import com.smartomni.common.entity.BaseTenantEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/** UC-28: Ho tro ky thuat cho Tenant. */
@Getter
@Setter
@Entity
@Table(name = "support_tickets")
public class SupportTicket extends BaseTenantEntity {

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "assigned_to")
    private Long assignedTo; // Super Admin xu ly

    private String subject;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    private TicketStatus status = TicketStatus.OPEN;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    public enum TicketStatus { OPEN, IN_PROGRESS, RESOLVED }
}
