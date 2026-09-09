package com.smartomni.catalog.entity;

import com.smartomni.common.entity.BaseTenantEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/** UC-08: SKU la don vi ban hang - moi bien the (size/mau) la 1 SKU. */
@Getter
@Setter
@Entity
@Table(name = "product_skus", uniqueConstraints = @UniqueConstraint(columnNames = {"tenant_id", "sku_code"}))
public class ProductSku extends BaseTenantEntity {

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "sku_code", nullable = false)
    private String skuCode;

    @Column(name = "variant_name")
    private String variantName;

    @Column(name = "base_price")
    private BigDecimal basePrice;
}
