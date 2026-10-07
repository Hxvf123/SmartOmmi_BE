package com.smartomni.order.repository;

import com.smartomni.order.entity.ReturnRefund;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

@org.springframework.transaction.annotation.Transactional(readOnly = true)
public interface ReturnRefundRepository extends JpaRepository<ReturnRefund, Long> {
    List<ReturnRefund> findByTenantId(Long tenantId);
}
