package com.smartomni.catalog.entity;

import org.hibernate.annotations.JdbcType;
import com.smartomni.common.persistence.LowercasePostgreSQLEnumJdbcType;
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
    @JdbcType(LowercasePostgreSQLEnumJdbcType.class)
    @Column(nullable = false, columnDefinition = "product_status")
    private ProductStatus status = ProductStatus.ACTIVE;

    @Column(name = "created_by")
    private Long createdBy;

    public enum ProductStatus { ACTIVE, HIDDEN }
}
