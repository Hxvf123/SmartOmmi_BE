package com.smartomni.inventory.repository;

import com.smartomni.inventory.entity.Inventory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

@org.springframework.transaction.annotation.Transactional(readOnly = true)
public interface InventoryRepository extends JpaRepository<Inventory, Long> {
    Optional<Inventory> findBySkuId(Long skuId);
}
