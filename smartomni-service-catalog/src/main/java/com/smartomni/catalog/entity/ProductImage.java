package com.smartomni.catalog.entity;

import com.smartomni.common.entity.BaseTenantEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/** UC-15: Quan ly hinh anh san pham - luu tren Cloud Storage theo prefix {tenant_id}/products/... */
@Getter
@Setter
@Entity
@Table(name = "product_images")
public class ProductImage extends BaseTenantEntity {

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(nullable = false)
    private String url;

    @Column(name = "sort_order")
    private Integer sortOrder = 0;
}
