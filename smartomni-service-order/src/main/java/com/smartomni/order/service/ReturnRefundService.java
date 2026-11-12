package com.smartomni.order.service;

import com.smartomni.common.exception.ResourceNotFoundException;
import com.smartomni.common.tenant.TenantContext;
import com.smartomni.order.dto.CreateReturnRequest;
import com.smartomni.order.entity.Order;
import com.smartomni.order.entity.OrderItem;
import com.smartomni.order.entity.ReturnRefund;
import com.smartomni.order.repository.OrderItemRepository;
import com.smartomni.order.repository.OrderRepository;
import com.smartomni.order.repository.ReturnRefundRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** UC-13: Quan ly hoan/huy don hang tap trung da san. FR-030. */
@Service
@RequiredArgsConstructor
public class ReturnRefundService {

    private final ReturnRefundRepository returnRefundRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    // private final InventoryServiceClient inventoryServiceClient; // TODO: cong tra ton kho khi hoan thanh

    @Transactional
    public ReturnRefund createReturnRequest(CreateReturnRequest request) {
        Long tenantId = requireTenantId();
        Order order = orderRepository.findByIdAndTenantId(request.getOrderId(), tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Don hang khong ton tai: " + request.getOrderId()));

        ReturnRefund refund = new ReturnRefund();
        refund.setTenantId(tenantId);
        refund.setOrderId(order.getId());
        refund.setReason(request.getReason());
        refund.setStatus(ReturnRefund.RefundStatus.REQUESTED);
        return returnRefundRepository.save(refund);
    }

    @Transactional
    public ReturnRefund approveAndRestock(Long refundId) {
        Long tenantId = requireTenantId();
        ReturnRefund refund = returnRefundRepository.findByIdAndTenantId(refundId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Yeu cau hoan/huy khong ton tai: " + refundId));
        Order order = orderRepository.findByIdAndTenantId(refund.getOrderId(), tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Don hang khong ton tai: " + refund.getOrderId()));

        refund.setStatus(ReturnRefund.RefundStatus.COMPLETED);
        refund.setRestocked(true);
        returnRefundRepository.save(refund);

        // FR-030: cong tra ton kho tuong ung
        for (OrderItem item : orderItemRepository.findByOrderIdAndTenantId(refund.getOrderId(), tenantId)) {
            // TODO: inventoryServiceClient.restock(item.getSkuId(), item.getQuantity());
        }

        order.setStatus(Order.OrderStatus.RETURNED);
        orderRepository.save(order);

        return refund;
    }

    private Long requireTenantId() {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new ResourceNotFoundException("Khong tim thay tenant hien tai");
        }
        return tenantId;
    }
}
