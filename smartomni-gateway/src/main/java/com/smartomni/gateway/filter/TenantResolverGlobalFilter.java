package com.smartomni.gateway.filter;

import com.smartomni.common.constant.AppConstants;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Resolve tenant_id truoc khi request duoc route toi cac service phia sau, theo 2 kich ban:
 *
 *  1) Storefront/Admin ERP: subdomain dang "{tenant-subdomain}.smartomni.vn"
 *     -> Gateway goi Tenant Service (hoac cache Redis) de lay tenant_id tu subdomain,
 *        sau do gan vao header noi bo X-Tenant-Id.
 *
 *  2) Webhook tu san TMDT: path dang "/webhook/{tenantId}/{platform}" (UC-32 / FR-057)
 *     -> trich xuat tenant_id truc tiep tu path, KHONG can tra cuu subdomain.
 *
 * Day la diem "TODO" quan trong nhat can hoan thien khi bat dau code that:
 * hien tai dang stub logic resolve (goi placeholder), can thay bang goi
 * that toi Tenant Service (qua WebClient) + cache Redis de tranh N+1 query.
 */
@Component
public class TenantResolverGlobalFilter implements GlobalFilter, Ordered {

    private static final Pattern WEBHOOK_PATH_PATTERN = Pattern.compile("^/webhook/(\\d+)/.*");

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        // Kich ban 2: Webhook path co san tenant_id
        Matcher matcher = WEBHOOK_PATH_PATTERN.matcher(path);
        if (matcher.matches()) {
            String tenantId = matcher.group(1);
            ServerHttpRequest mutated = request.mutate()
                    .header(AppConstants.HEADER_TENANT_ID, tenantId)
                    .build();
            return chain.filter(exchange.mutate().request(mutated).build());
        }

        // Kich ban 1: resolve tu subdomain (vd hxvf123.smartomni.vn -> tenant_id)
        String host = request.getURI().getHost();
        String subdomain = extractSubdomain(host);
        if (subdomain != null) {
            // TODO: thay the bang goi that: tenantLookupService.resolveTenantIdBySubdomain(subdomain)
            // Uu tien tra cuu qua Redis cache (key: "tenant:subdomain:{subdomain}") truoc khi goi Tenant Service.
            String resolvedTenantId = resolveTenantIdBySubdomainStub(subdomain);
            if (resolvedTenantId != null) {
                ServerHttpRequest mutated = request.mutate()
                        .header(AppConstants.HEADER_TENANT_ID, resolvedTenantId)
                        .build();
                return chain.filter(exchange.mutate().request(mutated).build());
            }
        }

        return chain.filter(exchange);
    }

    private String extractSubdomain(String host) {
        if (host == null) return null;
        String[] parts = host.split("\\.");
        // vd: hxvf123.smartomni.vn -> ["hxvf123", "smartomni", "vn"]
        if (parts.length >= 3) {
            return parts[0];
        }
        return null;
    }

    private String resolveTenantIdBySubdomainStub(String subdomain) {
        // STUB - can thay bang tra cuu that (Redis cache -> fallback Tenant Service)
        return null;
    }

    @Override
    public int getOrder() {
        return -1; // chay truoc cac filter khac
    }
}
