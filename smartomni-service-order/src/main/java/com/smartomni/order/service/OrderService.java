package com.smartomni.order.service;

import com.smartomni.common.exception.ResourceNotFoundException;
import com.smartomni.order.client.InventoryServiceClient;
import com.smartomni.order.dto.WebhookOrderPayload;
import com.smartomni.order.entity.Order;
import com.smartomni.order.entity.OrderItem;
import com.smartomni.order.repository.OrderItemRepository;
import com.smartomni.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * UC-33: Xu ly bat dong bo don hang (Async Order Processing) - FR-061,062
 * UC-10: Theo doi & xu ly don hang - FR-023,024,025
 * UC-06: Tra cuu don hang cheo nen tang (Khach hang)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final InventoryServiceClient inventoryServiceClient;

    /**
     * Duoc goi boi RabbitMQ Consumer (OrderMessageConsumer) - noi dung chinh cua UC-33.
     * FR-025: kiem tra idempotency truoc khi tao moi (Webhook va Polling co the trung nhau).
     */
    @Transactional
    public void processIncomingOrder(WebhookOrderPayload payload, Order.OrderSource source) {
        Order.Platform platform = Order.Platform.valueOf(payload.getPlatform());

        Optional<Order> existing = orderRepository.findByTenantIdAndPlatformAndPlatformOrderId(
                payload.getTenantId(), platform, payload.getPlatformOrderId());

        if (existing.isPresent()) {
            log.info("Don hang {} da ton tai (tenant={}, source={}) - bo qua de tranh trung lap",
                    payload.getPlatformOrderId(), payload.getTenantId(), source);
            return; // FR-025: idempotency - khong tao trung
        }

        Order order = new Order();
        order.setTenantId(payload.getTenantId());
        order.setPlatform(platform);
        order.setPlatformOrderId(payload.getPlatformOrderId());
        order.setCustomerName(payload.getCustomerName());
        order.setCustomerPhone(payload.getCustomerPhone());
        order.setShippingAddress(payload.getShippingAddress());
        order.setTotalAmount(payload.getTotalAmount());
        order.setSource(source);
        order.setStatus(Order.OrderStatus.CONFIRMED);
        order = orderRepository.save(order);

        for (var itemPayload : payload.getItems()) {
            OrderItem item = new OrderItem();
            item.setOrderId(order.getId());
            item.setSkuId(itemPayload.getSkuId());
            item.setQuantity(itemPayload.getQuantity());
            item.setUnitPrice(itemPayload.getUnitPrice());
            orderItemRepository.save(item);

            // FR-027, FR-028: goi Inventory Service tru kho (co Optimistic Lock + Outbox ben trong)
            try {
                inventoryServiceClient.deductStock(itemPayload.getSkuId(), itemPayload.getQuantity());
            } catch (Exception ex) {
                log.error("Tru kho that bai cho SKU {}: {}", itemPayload.getSkuId(), ex.getMessage());
                // TODO: danh dau don hang can xu ly ngoai le (oversold) - UC-33/A2
            }
        }
    }

    public Page<Order> getOrdersByTenant(Long tenantId, Pageable pageable) {
        return orderRepository.findByTenantId(tenantId, pageable);
    }

    @Transactional
    public Order updateStatus(Long orderId, Order.OrderStatus newStatus) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Don hang khong ton tai: " + orderId));
        order.setStatus(newStatus);
        return orderRepository.save(order);
    }

    /** UC-06: Khach hang tra cuu don hang bang Order ID, khong can dang nhap. */
    public Order trackOrder(String platform, String platformOrderId) {
        return orderRepository.findByPlatformAndPlatformOrderId(Order.Platform.valueOf(platform), platformOrderId)
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay don hang"));
    }
}
