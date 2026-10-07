package com.smartomni.order.repository;

import com.smartomni.order.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

@org.springframework.transaction.annotation.Transactional(readOnly = true)
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
    List<OrderItem> findByOrderId(Long orderId);
}
