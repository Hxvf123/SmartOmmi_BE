package com.smartomni.ai.entity;

import com.smartomni.common.entity.BaseTenantEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

/** UC-14/UC-35: Ket qua Demand Forecasting (Prophet/ARIMA) tra ve tu AI Service (Python). */
@Getter
@Setter
@Entity
@Table(name = "demand_forecasts")
public class DemandForecast extends BaseTenantEntity {

    @Column(name = "sku_id", nullable = false)
    private Long skuId;

    @Column(name = "model_version_id")
    private Long modelVersionId;

    @Column(name = "forecast_date")
    private LocalDate forecastDate;

    @Column(name = "predicted_quantity")
    private Integer predictedQuantity;

    @Column(name = "recommended_reorder_qty")
    private Integer recommendedReorderQty;
}
