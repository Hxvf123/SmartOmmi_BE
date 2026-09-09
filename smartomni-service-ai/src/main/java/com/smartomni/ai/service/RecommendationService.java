package com.smartomni.ai.service;

import com.smartomni.ai.client.PythonAiServiceClient;
import com.smartomni.ai.dto.RecommendationResultDto;
import com.smartomni.ai.entity.ProductRecommendationCache;
import com.smartomni.ai.repository.ProductRecommendationCacheRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** UC-07/UC-36: Goi y san pham cheo (Cross-selling Engine) - FR-016, FR-068. */
@Service
@RequiredArgsConstructor
public class RecommendationService {

    private final PythonAiServiceClient pythonAiServiceClient;
    private final ProductRecommendationCacheRepository cacheRepository;

    /** Doc tu cache truoc (da duoc tinh san theo batch); neu chua co, goi Python service tinh ngay (fallback). */
    public List<ProductRecommendationCache> getTopRecommendations(Long tenantId, Long skuId) {
        List<ProductRecommendationCache> cached = cacheRepository.findTop3BySourceSkuIdOrderBySimilarityScoreDesc(skuId);
        if (!cached.isEmpty()) {
            return cached;
        }
        return refreshRecommendations(tenantId, skuId);
    }

    @Transactional
    public List<ProductRecommendationCache> refreshRecommendations(Long tenantId, Long skuId) {
        List<RecommendationResultDto> results = pythonAiServiceClient.requestRecommendations(tenantId, skuId);

        return results.stream().map(result -> {
            ProductRecommendationCache cache = new ProductRecommendationCache();
            cache.setTenantId(tenantId);
            cache.setSourceSkuId(skuId);
            cache.setRecommendedSkuId(result.getRecommendedSkuId());
            cache.setSimilarityScore(result.getSimilarityScore());
            return cacheRepository.save(cache);
        }).toList();
    }
}
