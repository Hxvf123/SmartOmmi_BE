package com.smartomni.order.service;

import com.smartomni.common.exception.ResourceNotFoundException;
import com.smartomni.common.tenant.TenantContext;
import com.smartomni.order.dto.CreateReturnRequest;
import com.smartomni.order.entity.Order;
import com.smartomni.order.entity.ReturnRefund;
import com.smartomni.order.repository.OrderItemRepository;
import com.smartomni.order.repository.OrderRepository;
import com.smartomni.order.repository.ReturnRefundRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReturnRefundServiceTest {
    @Mock ReturnRefundRepository refunds;
    @Mock OrderRepository orders;
    @Mock OrderItemRepository items;
    @InjectMocks ReturnRefundService service;

    @AfterEach void clear() { TenantContext.clear(); }

    @Test void cannotCreateRefundForAnotherTenantOrder() {
        TenantContext.setTenantId(2L);
        CreateReturnRequest request = new CreateReturnRequest();
        request.setOrderId(7L);
        when(orders.findByIdAndTenantId(7L, 2L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.createReturnRequest(request));
        verifyNoInteractions(refunds, items);
    }

    @Test void cannotApproveAnotherTenantRefund() {
        TenantContext.setTenantId(2L);
        when(refunds.findByIdAndTenantId(8L, 2L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.approveAndRestock(8L));
        verifyNoInteractions(orders, items);
    }

    @Test void ownTenantRefundUsesOwnOrderAndItems() {
        TenantContext.setTenantId(2L);
        ReturnRefund refund = new ReturnRefund();
        refund.setOrderId(7L);
        Order order = new Order();
        order.setId(7L);
        when(refunds.findByIdAndTenantId(8L, 2L)).thenReturn(Optional.of(refund));
        when(orders.findByIdAndTenantId(7L, 2L)).thenReturn(Optional.of(order));

        service.approveAndRestock(8L);

        verify(items).findByOrderIdAndTenantId(7L, 2L);
        verify(orders).save(order);
    }
}
