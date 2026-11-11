package com.smartomni.inventory.repository;

import com.smartomni.inventory.entity.Inventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

@org.springframework.transaction.annotation.Transactional(readOnly = true)
public interface InventoryRepository extends JpaRepository<Inventory, Long> {
    Page<Inventory> findByTenantId(Long tenantId, Pageable pageable);
    Optional<Inventory> findBySkuId(Long skuId);
}
