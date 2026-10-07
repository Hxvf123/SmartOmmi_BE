package com.smartomni.order.repository;

import com.smartomni.order.entity.WebhookEventLog;
import org.springframework.data.jpa.repository.JpaRepository;

@org.springframework.transaction.annotation.Transactional(readOnly = true)
public interface WebhookEventLogRepository extends JpaRepository<WebhookEventLog, Long> {
}
