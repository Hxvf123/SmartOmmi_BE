package com.smartomni.inventory.scheduler;

import com.smartomni.inventory.entity.InventoryOutboxEvent;
import com.smartomni.inventory.repository.InventoryOutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;
import com.smartomni.common.tenant.TenantContext;

import java.time.Instant;
import java.util.List;

/**
 * UC-40: Outbox Publisher/Relay - doc cac ban ghi Outbox chua xu ly va goi
 * API UpdateStock tuong ung len tung san TMDT. FR-075, FR-076.
 *
 * TODO: thay logic goi API that (qua smartomni-service-integration) va ap dung
 * Exponential Backoff khi that bai (hien tai chi tang retryCount don gian).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxPublisherScheduler {

    private static final int MAX_RETRY = 5;

    private final InventoryOutboxEventRepository outboxEventRepository;
    private final TransactionTemplate transactionTemplate;
    // private final IntegrationServiceClient integrationServiceClient; // TODO: Feign client

    public void publishPendingEvents() {
        try (var scope = TenantContext.openScope(null, "outbox_worker", null)) {
            transactionTemplate.executeWithoutResult(status -> publishInTransaction());
        }
    }

    private void publishInTransaction() {
        List<InventoryOutboxEvent> pendingEvents =
                outboxEventRepository.findTop100ByStatusOrderByCreatedAtAsc(InventoryOutboxEvent.OutboxStatus.PENDING);

        for (InventoryOutboxEvent event : pendingEvents) {
            try {
                // TODO: integrationServiceClient.updateStock(event.getTenantId(), event.getPlatform(), event.getSkuId(), event.getQuantity());
                event.setStatus(InventoryOutboxEvent.OutboxStatus.SENT);
                event.setProcessedAt(Instant.now());
            } catch (Exception ex) {
                log.warn("Outbox publish that bai cho event id={}: {}", event.getId(), ex.getMessage());
                event.setRetryCount(event.getRetryCount() + 1);
                if (event.getRetryCount() >= MAX_RETRY) {
                    event.setStatus(InventoryOutboxEvent.OutboxStatus.FAILED); // FR-076: loi vinh vien, can Manager kiem tra
                }
            }
            outboxEventRepository.save(event);
        }
    }
}
