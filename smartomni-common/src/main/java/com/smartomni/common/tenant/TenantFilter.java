package com.smartomni.common.tenant;

import com.smartomni.common.constant.AppConstants;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Doc header noi bo X-Tenant-Id (do API Gateway gan vao sau khi resolve
 * tu subdomain / webhook path / JWT claim) va nap vao TenantContext cho
 * toan bo vong doi request. Neu service duoc goi truc tiep (khong qua
 * Gateway, vd trong moi truong dev/test) van co the truyen header nay
 * thu cong de mo phong tenant.
 */
@Component
@Order(1)
public class TenantFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                     HttpServletResponse response,
                                     FilterChain filterChain) throws ServletException, IOException {
        try {
            String tenantHeader = request.getHeader(AppConstants.HEADER_TENANT_ID);
            if (tenantHeader != null && !tenantHeader.isBlank()) {
                TenantContext.setTenantId(Long.valueOf(tenantHeader));
            }

            String roleHeader = request.getHeader(AppConstants.HEADER_USER_ROLE);
            if (roleHeader != null) {
                TenantContext.setCurrentRole(roleHeader);
            }

            String userHeader = request.getHeader(AppConstants.HEADER_USER_ID);
            if (userHeader != null && !userHeader.isBlank()) {
                TenantContext.setCurrentUserId(Long.valueOf(userHeader));
            }

            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }
}
