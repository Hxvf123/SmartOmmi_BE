package com.smartomni.catalog.entity;

import com.smartomni.common.entity.BaseTenantEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/** UC-16: Khai bao su kien khuyen mai - la bien ngoai sinh h(t) cho mo hinh Prophet ben AI Service. */
@Getter
@Setter
@Entity
@Table(name = "promotion_events")
public class PromotionEvent extends BaseTenantEntity {

    private String name;

    @Column(name = "discount_percent")
    private BigDecimal discountPercent;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "applies_to")
    private PromotionScope appliesTo = PromotionScope.ALL;

    @Column(name = "sku_id")
    private Long skuId; // null neu appliesTo = ALL/CATEGORY

    @Column(name = "created_by")
    private Long createdBy;

    public enum PromotionScope { ALL, CATEGORY, SKU }
}
