package com.smartomni.catalog.repository;

import com.smartomni.catalog.entity.ProductMarketplaceLink;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

@org.springframework.transaction.annotation.Transactional(readOnly = true)
public interface ProductMarketplaceLinkRepository extends JpaRepository<ProductMarketplaceLink, Long> {
    Optional<ProductMarketplaceLink> findBySkuIdAndPlatform(Long skuId, ProductMarketplaceLink.Platform platform);
    List<ProductMarketplaceLink> findBySkuId(Long skuId);
}
