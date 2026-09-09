package com.smartomni.catalog.repository;

import com.smartomni.catalog.entity.ProductSku;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProductSkuRepository extends JpaRepository<ProductSku, Long> {
    boolean existsByTenantIdAndSkuCode(Long tenantId, String skuCode);
    Optional<ProductSku> findByTenantIdAndSkuCode(Long tenantId, String skuCode); // dung cho upsert (UC-42/FR-085)
    List<ProductSku> findByProductId(Long productId);
}
