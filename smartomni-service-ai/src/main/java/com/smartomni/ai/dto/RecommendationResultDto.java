package com.smartomni.ai.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class RecommendationResultDto {
    private Long recommendedSkuId;
    private BigDecimal similarityScore;
}
