package com.smartomni.integration.repository;

import com.smartomni.integration.entity.MarketplaceConnection;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

@org.springframework.transaction.annotation.Transactional(readOnly = true)
public interface MarketplaceConnectionRepository extends JpaRepository<MarketplaceConnection, Long> {
    Optional<MarketplaceConnection> findByTenantIdAndPlatform(Long tenantId, MarketplaceConnection.Platform platform);
    List<MarketplaceConnection> findByTenantId(Long tenantId);
    List<MarketplaceConnection> findByStatus(MarketplaceConnection.ConnectionStatus status); // dung cho Polling Scheduler (UC-34) va Price Sync (UC-43)
}
