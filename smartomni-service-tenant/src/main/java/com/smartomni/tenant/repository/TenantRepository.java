package com.smartomni.tenant.repository;

import com.smartomni.tenant.entity.Tenant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TenantRepository extends JpaRepository<Tenant, Long> {
    boolean existsBySubdomain(String subdomain);
    Optional<Tenant> findBySubdomain(String subdomain);

    // UC-25: Quan ly danh sach Tenant - tim kiem/loc theo ten, trang thai
    Page<Tenant> findByNameContainingIgnoreCaseOrSubdomainContainingIgnoreCase(String name, String subdomain, Pageable pageable);
}
