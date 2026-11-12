package com.smartomni.common.tenant;

import com.smartomni.common.constant.AppConstants;
import com.smartomni.common.security.PublicEndpointPaths;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/**
 * Filter trích xuất subdomain từ header X-Tenant-Subdomain hoặc từ Host/X-Forwarded-Host
 * (hỗ trợ *.smartomni.vn, *.lvh.me, *.localhost), tra cứu DB để nạp tenant_id và
 * subscription_plan vào TenantContext cho phiên truy cập & kích hoạt PostgreSQL RLS.
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
public class SubdomainContextFilter extends OncePerRequestFilter {

    private final TenantProperties tenantProperties;
    private final TenantPlanResolver tenantPlanResolver;

    public SubdomainContextFilter(TenantProperties tenantProperties) {
        this(tenantProperties, null);
    }

    public SubdomainContextFilter(TenantProperties tenantProperties, TenantPlanResolver tenantPlanResolver) {
        this.tenantProperties = tenantProperties != null ? tenantProperties : new TenantProperties();
        this.tenantPlanResolver = tenantPlanResolver;
    }

    @Autowired
    public SubdomainContextFilter(ObjectProvider<TenantProperties> tenantPropertiesProvider,
                                  ObjectProvider<TenantPlanResolver> tenantPlanResolverProvider) {
        TenantProperties props = tenantPropertiesProvider != null ? tenantPropertiesProvider.getIfAvailable() : null;
        this.tenantProperties = props != null ? props : new TenantProperties();
        this.tenantPlanResolver = tenantPlanResolverProvider != null ? tenantPlanResolverProvider.getIfAvailable() : null;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return PublicEndpointPaths.isBypassedForTenantFilters(request);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String existingSubdomain = TenantContext.getCurrentSubdomain();
        Long existingTenantId = TenantContext.getTenantId();
        String existingPlan = TenantContext.getSubscriptionPlan();

        boolean subdomainSetByThisFilter = false;
        boolean tenantIdSetByThisFilter = false;
        boolean planSetByThisFilter = false;

        try {
            if (existingSubdomain == null || existingSubdomain.isBlank()) {
                String extractedSubdomain = extractSubdomain(request);
                if (extractedSubdomain != null && isSubdomainAllowed(extractedSubdomain)) {
                    TenantContext.setCurrentSubdomain(extractedSubdomain);
                    subdomainSetByThisFilter = true;
                    log.debug("SubdomainContextFilter resolved subdomain: {}", extractedSubdomain);
                }
            }

            if (tenantPlanResolver != null) {
                String currentSubdomain = TenantContext.getCurrentSubdomain();
                Long currentTenantId = TenantContext.getTenantId();
                String currentPlan = TenantContext.getSubscriptionPlan();
                boolean missingPlan = currentPlan == null || currentPlan.isBlank();

                if (currentTenantId == null && currentSubdomain != null && !currentSubdomain.isBlank()) {
                    Optional<TenantPlanResolver.TenantLookupResult> resolved =
                            tenantPlanResolver.findTenantBySubdomain(currentSubdomain);
                    if (resolved.isPresent()) {
                        TenantPlanResolver.TenantLookupResult tenant = resolved.get();
                        TenantContext.setTenantId(tenant.id());
                        tenantIdSetByThisFilter = true;
                        if (missingPlan) {
                            TenantContext.setSubscriptionPlan(tenant.subscriptionPlan());
                            planSetByThisFilter = true;
                        }
                        log.debug("SubdomainContextFilter loaded tenantId={} plan={} for subdomain={}",
                                tenant.id(), tenant.subscriptionPlan(), currentSubdomain);
                    }
                } else if (currentTenantId != null && missingPlan) {
                    Optional<TenantPlanResolver.TenantLookupResult> resolved =
                            tenantPlanResolver.findTenantById(currentTenantId);
                    if (resolved.isPresent()) {
                        TenantPlanResolver.TenantLookupResult tenant = resolved.get();
                        TenantContext.setSubscriptionPlan(tenant.subscriptionPlan());
                        planSetByThisFilter = true;
                        if ((currentSubdomain == null || currentSubdomain.isBlank()) && tenant.subdomain() != null) {
                            TenantContext.setCurrentSubdomain(tenant.subdomain());
                            subdomainSetByThisFilter = true;
                        }
                        log.debug("SubdomainContextFilter loaded plan={} for tenantId={}",
                                tenant.subscriptionPlan(), currentTenantId);
                    }
                } else if (currentSubdomain != null && !currentSubdomain.isBlank() && missingPlan) {
                    Optional<TenantPlanResolver.TenantLookupResult> resolved =
                            tenantPlanResolver.findTenantBySubdomain(currentSubdomain);
                    if (resolved.isPresent()) {
                        TenantContext.setSubscriptionPlan(resolved.get().subscriptionPlan());
                        planSetByThisFilter = true;
                    }
                }
            }

            filterChain.doFilter(request, response);
        } finally {
            if (subdomainSetByThisFilter) {
                TenantContext.setCurrentSubdomain(existingSubdomain);
            }
            if (tenantIdSetByThisFilter) {
                TenantContext.setTenantId(existingTenantId);
            }
            if (planSetByThisFilter) {
                TenantContext.setSubscriptionPlan(existingPlan);
            }
        }
    }

    /**
     * Ưu tiên trích xuất từ header X-Tenant-Subdomain, nếu không có thì phân tích từ Host / X-Forwarded-Host.
     */
    public String extractSubdomain(HttpServletRequest request) {
        // 1. Kiểm tra header X-Tenant-Subdomain
        String headerSubdomain = request.getHeader(AppConstants.HEADER_TENANT_SUBDOMAIN);
        if (headerSubdomain != null && !headerSubdomain.isBlank()) {
            return headerSubdomain.trim().toLowerCase(Locale.ROOT);
        }

        // 2. Kiểm tra X-Forwarded-Host hoặc Host
        String host = request.getHeader("X-Forwarded-Host");
        if (host == null || host.isBlank()) {
            host = request.getHeader("Host");
        }
        if (host == null || host.isBlank()) {
            host = request.getServerName();
        }

        return parseSubdomainFromHost(host);
    }

    public String parseSubdomainFromHost(String host) {
        if (host == null || host.isBlank()) {
            return null;
        }

        String cleanHost = host.trim();
        if (cleanHost.contains(",")) {
            cleanHost = cleanHost.substring(0, cleanHost.indexOf(',')).trim();
        }
        cleanHost = cleanHost.toLowerCase(Locale.ROOT);
        // Loại bỏ cổng port nếu có (ví dụ: apple.lvh.me:8085 -> apple.lvh.me)
        if (cleanHost.contains(":")) {
            cleanHost = cleanHost.substring(0, cleanHost.indexOf(':'));
        }

        for (String baseDomain : collectSupportedBaseDomains()) {
            String suffix = "." + baseDomain;
            if (cleanHost.endsWith(suffix)) {
                String prefix = cleanHost.substring(0, cleanHost.length() - suffix.length());
                if (prefix.contains(".")) {
                    String[] parts = prefix.split("\\.");
                    return parts[parts.length - 1];
                }
                return prefix;
            }
        }

        return null;
    }

    private Set<String> collectSupportedBaseDomains() {
        Set<String> domains = new LinkedHashSet<>();
        if (tenantProperties.getBaseDomain() != null && !tenantProperties.getBaseDomain().isBlank()) {
            domains.add(tenantProperties.getBaseDomain().trim().toLowerCase(Locale.ROOT));
        }
        if (tenantProperties.getSupportedBaseDomains() != null) {
            for (String d : tenantProperties.getSupportedBaseDomains()) {
                if (d != null && !d.isBlank()) {
                    domains.add(d.trim().toLowerCase(Locale.ROOT));
                }
            }
        }
        domains.add("smartomni.vn");
        domains.add("lvh.me");
        domains.add("localhost");
        return domains;
    }

    public boolean isSubdomainAllowed(String subdomain) {
        if (subdomain == null || subdomain.isBlank()) {
            return false;
        }
        if (subdomain.length() < 3 || subdomain.length() > 30) {
            return false;
        }
        if (!subdomain.matches("^[a-z0-9-]+$")) {
            return false;
        }

        Set<String> reserved = tenantProperties.getReservedSubdomains();
        if (reserved != null && (reserved.contains(subdomain) || reserved.stream().anyMatch(subdomain::equalsIgnoreCase))) {
            return false;
        }

        return true;
    }
}
