package com.smartomni.inventory.service;

import com.smartomni.common.exception.BusinessException;
import com.smartomni.common.tenant.TenantContext;
import com.smartomni.inventory.entity.Inventory;
import com.smartomni.inventory.repository.InventoryOutboxEventRepository;
import com.smartomni.inventory.repository.InventoryRepository;
import com.smartomni.inventory.repository.SkuOwnershipRepository;
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
class InventoryServiceTest {
    @Mock InventoryRepository inventoryRepository;
    @Mock InventoryOutboxEventRepository outboxEventRepository;
    @Mock SkuOwnershipRepository skuOwnershipRepository;
    @InjectMocks InventoryService service;

    @AfterEach void clearContext() { TenantContext.clear(); }

    @Test void foreignSkuCannotChangeStockOrCreateOutbox() {
        TenantContext.setTenantId(1L);
        when(inventoryRepository.findByTenantIdAndSkuId(1L, 9L)).thenReturn(Optional.empty());
        when(skuOwnershipRepository.exists(1L, 9L)).thenReturn(false);

        assertThrows(BusinessException.class, () -> service.adjustStock(9L, 5));
        assertThrows(BusinessException.class, () -> service.restock(9L, 2));
        assertThrows(BusinessException.class, () -> service.deductStock(9L, 1));
        verify(inventoryRepository, never()).save(any());
        verifyNoInteractions(outboxEventRepository);
    }

    @Test void ownedSkuWithoutInventoryCanBeCreated() {
        TenantContext.setTenantId(1L);
        when(inventoryRepository.findByTenantIdAndSkuId(1L, 9L)).thenReturn(Optional.empty());
        when(skuOwnershipRepository.exists(1L, 9L)).thenReturn(true);

        service.adjustStock(9L, 5);

        verify(inventoryRepository).save(argThat(i -> i.getTenantId().equals(1L)
                && i.getSkuId().equals(9L) && i.getQuantityOnHand().equals(5)));
        verify(outboxEventRepository).save(any());
    }

    @Test void ownSkuCanBeAdjusted() {
        TenantContext.setTenantId(1L);
        Inventory inventory = new Inventory();
        inventory.setTenantId(1L);
        inventory.setSkuId(9L);
        when(inventoryRepository.findByTenantIdAndSkuId(1L, 9L)).thenReturn(Optional.of(inventory));

        service.adjustStock(9L, 5);

        assertEquals(5, inventory.getQuantityOnHand());
        verify(inventoryRepository).save(inventory);
        verify(outboxEventRepository).save(any());
    }

    @Test void missingTenantIsRejectedBeforeLookup() {
        assertThrows(BusinessException.class, () -> service.adjustStock(9L, 5));
        verifyNoInteractions(inventoryRepository, outboxEventRepository);
    }
}
