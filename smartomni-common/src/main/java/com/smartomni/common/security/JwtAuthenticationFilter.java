package com.smartomni.common.security;

import com.smartomni.common.constant.AppConstants;
import com.smartomni.common.tenant.TenantContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Đọc Bearer token từ header Authorization, kiểm tra blacklist Redis,
 * xác thực và nạp thông tin user (id, role, tenantId) vào SecurityContext & TenantContext.
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final TokenBlacklistValidator tokenBlacklistValidator;

    public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider) {
        this(jwtTokenProvider, null);
    }

    public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider, TokenBlacklistValidator tokenBlacklistValidator) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.tokenBlacklistValidator = tokenBlacklistValidator;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                     HttpServletResponse response,
                                     FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader(AppConstants.HEADER_AUTHORIZATION);

        if (header != null && header.startsWith(AppConstants.BEARER_PREFIX)) {
            String token = header.substring(AppConstants.BEARER_PREFIX.length());

            // Kiểm tra token có trong Redis Blacklist không (sau khi Logout)
            if (tokenBlacklistValidator != null && tokenBlacklistValidator.isBlacklisted(token)) {
                filterChain.doFilter(request, response);
                return;
            }

            if (jwtTokenProvider.validateToken(token)) {
                Long userId = jwtTokenProvider.getUserId(token);
                String role = jwtTokenProvider.getRole(token);
                Long tenantId = jwtTokenProvider.getTenantId(token);

                TenantContext.setCurrentUserId(userId);
                TenantContext.setCurrentRole(role);
                TenantContext.setTenantId(tenantId);

                var authentication = new UsernamePasswordAuthenticationToken(
                        userId, null, List.of(new SimpleGrantedAuthority("ROLE_" + role)));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        }

        filterChain.doFilter(request, response);
    }
}
