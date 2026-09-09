package com.smartomni.ai.entity;

import com.smartomni.common.entity.BaseTenantEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * UC-37: Huan luyen & Rollback mo hinh AI theo Tenant.
 * UC-31: Xem lich su phien ban mo hinh AI theo Tenant.
 * Registry tenant_id -> model version dang active, kem RMSE de so sanh khi retrain.
 */
@Getter
@Setter
@Entity
@Table(name = "ai_model_versions")
public class AiModelVersion extends BaseTenantEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "model_type", nullable = false)
    private ModelType modelType;

    @Column(name = "version_number")
    private String versionNumber;

    private BigDecimal rmse;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ModelStatus status = ModelStatus.TRAINING;

    @Column(name = "trained_at")
    private Instant trainedAt;

    public enum ModelType { DEMAND_FORECASTING, RECOMMENDATION }
    public enum ModelStatus { TRAINING, ACTIVE, ROLLED_BACK }
}
