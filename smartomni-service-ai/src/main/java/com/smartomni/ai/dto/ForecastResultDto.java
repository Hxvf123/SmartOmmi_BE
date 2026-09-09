package com.smartomni.ai.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class ForecastResultDto {
    private Long skuId;
    private LocalDate forecastDate;
    private Integer predictedQuantity;
    private Integer recommendedReorderQty;
    private java.math.BigDecimal rmse;
    private String modelVersion;
}
