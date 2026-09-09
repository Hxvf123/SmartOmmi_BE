package com.smartomni.order.repository;

import com.smartomni.order.entity.ReturnRefund;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReturnRefundRepository extends JpaRepository<ReturnRefund, Long> {
    List<ReturnRefund> findByTenantId(Long tenantId);
}
