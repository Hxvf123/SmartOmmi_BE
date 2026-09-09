package com.smartomni.ai.repository;

import com.smartomni.ai.entity.AiModelVersion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AiModelVersionRepository extends JpaRepository<AiModelVersion, Long> {
    List<AiModelVersion> findByTenantIdAndModelTypeOrderByTrainedAtDesc(Long tenantId, AiModelVersion.ModelType modelType);
    Optional<AiModelVersion> findByTenantIdAndModelTypeAndStatus(Long tenantId, AiModelVersion.ModelType modelType, AiModelVersion.ModelStatus status);
}
