package com.smartomni.common.security;

import com.smartomni.common.constant.AppConstants;
import com.smartomni.common.tenant.TenantContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

/**
 * Doc Bearer token tu header Authorization, xac thuc va nap thong tin
 * user (id, role, tenantId) vao ca Spring SecurityContext lan TenantContext.
 * Dat sau TenantFilter trong filter chain cua tung service duoc goi truc
 * tiep (khong qua Gateway) - vi du service duoc goi noi bo.
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;

    public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider) {
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                     HttpServletResponse response,
                                     FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader(AppConstants.HEADER_AUTHORIZATION);

        if (header != null && header.startsWith(AppConstants.BEARER_PREFIX)) {
            String token = header.substring(AppConstants.BEARER_PREFIX.length());

            if (jwtTokenProvider.validateToken(token)) {
                Long userId = jwtTokenProvider.getUserId(token);
                String role = jwtTokenProvider.getRole(token);
                Long tenantId = jwtTokenProvider.getTenantId(token);

                TenantContext.setCurrentUserId(userId);
                TenantContext.setCurrentRole(role);
                if (tenantId != null) {
                    TenantContext.setTenantId(tenantId);
                }

                var authentication = new UsernamePasswordAuthenticationToken(
                        userId, null, Collections.emptyList());
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        }

        filterChain.doFilter(request, response);
    }
}
