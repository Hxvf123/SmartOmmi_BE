package com.smartomni.ai.repository;

import com.smartomni.ai.entity.ProductRecommendationCache;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

@org.springframework.transaction.annotation.Transactional(readOnly = true)
public interface ProductRecommendationCacheRepository extends JpaRepository<ProductRecommendationCache, Long> {
    List<ProductRecommendationCache> findTop3BySourceSkuIdOrderBySimilarityScoreDesc(Long sourceSkuId);
}
