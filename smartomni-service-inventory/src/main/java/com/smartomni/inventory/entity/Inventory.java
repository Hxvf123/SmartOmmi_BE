package com.smartomni.inventory.entity;

import com.smartomni.common.entity.BaseTenantEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/** UC-11: Cap nhat ton kho. Cot "version" dung cho Optimistic Concurrency (FR-027). */
@Getter
@Setter
@Entity
@Table(name = "inventory")
public class Inventory extends BaseTenantEntity {

    @Column(name = "sku_id", nullable = false, unique = true)
    private Long skuId;

    @Column(name = "quantity_on_hand")
    private Integer quantityOnHand = 0;

    @Column(name = "reserved_quantity")
    private Integer reservedQuantity = 0;

    @Version
    private Integer version; // Optimistic locking - JPA tu dong quan ly
}
