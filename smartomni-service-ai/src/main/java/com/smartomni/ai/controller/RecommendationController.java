package com.smartomni.ai.controller;

import com.smartomni.ai.entity.ProductRecommendationCache;
import com.smartomni.ai.service.RecommendationService;
import com.smartomni.common.dto.ApiResponse;
import com.smartomni.common.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** UC-07: Nhan goi y san pham ca nhan hoa (Khach hang doc qua Storefront -> Gateway -> service nay). */
@RestController
@RequestMapping("/api/ai/recommendations")
@RequiredArgsConstructor
public class RecommendationController {

    private final RecommendationService recommendationService;

    @GetMapping("/{skuId}")
    public ApiResponse<List<ProductRecommendationCache>> getRecommendations(@PathVariable Long skuId) {
        return ApiResponse.success(recommendationService.getTopRecommendations(TenantContext.getTenantId(), skuId));
    }
}
