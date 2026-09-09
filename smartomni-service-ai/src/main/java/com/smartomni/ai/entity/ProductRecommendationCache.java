package com.smartomni.ai.entity;

import com.smartomni.common.entity.BaseTenantEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/** UC-07/UC-36: Cache ket qua Cosine Similarity - top san pham lien quan cho tung SKU. */
@Getter
@Setter
@Entity
@Table(name = "product_recommendations_cache")
public class ProductRecommendationCache extends BaseTenantEntity {

    @Column(name = "source_sku_id", nullable = false)
    private Long sourceSkuId;

    @Column(name = "recommended_sku_id", nullable = false)
    private Long recommendedSkuId;

    @Column(name = "similarity_score")
    private BigDecimal similarityScore;
}
