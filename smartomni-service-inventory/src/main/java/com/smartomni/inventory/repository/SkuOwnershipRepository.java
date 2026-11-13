package com.smartomni.inventory.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class SkuOwnershipRepository {
    private final JdbcTemplate jdbcTemplate;

    public boolean exists(Long tenantId, Long skuId) {
        return Boolean.TRUE.equals(jdbcTemplate.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM product_skus WHERE tenant_id = ? AND id = ?)",
                Boolean.class, tenantId, skuId));
    }
}
