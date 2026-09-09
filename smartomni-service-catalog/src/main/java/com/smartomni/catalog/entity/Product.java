package com.smartomni.catalog.entity;

import com.smartomni.common.entity.BaseTenantEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/** UC-08: Quan ly san pham (Catalog Management). */
@Getter
@Setter
@Entity
@Table(name = "products")
public class Product extends BaseTenantEntity {

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProductStatus status = ProductStatus.ACTIVE;

    @Column(name = "created_by")
    private Long createdBy;

    public enum ProductStatus { ACTIVE, HIDDEN }
}
