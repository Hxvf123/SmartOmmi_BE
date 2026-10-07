package com.smartomni.tenant.repository;

import com.smartomni.tenant.entity.SupportTicket;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

@org.springframework.transaction.annotation.Transactional(readOnly = true)
public interface SupportTicketRepository extends JpaRepository<SupportTicket, Long> {
    List<SupportTicket> findByTenantId(Long tenantId);
}
