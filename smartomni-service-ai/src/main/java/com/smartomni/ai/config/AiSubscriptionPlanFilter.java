package com.smartomni.ai.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartomni.common.dto.ApiResponse;
import com.smartomni.common.exception.FeatureNotAvailableException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Filter chặn sớm toàn bộ các API /api/v1/ai/forecasting/**, /api/v1/ai/forecast/**
 * và /api/v1/ai/recommendations/** nếu gói cước hiện tại của tenant là FREE (403 Forbidden).
 */
@Component
@Order(Ordered.LOWEST_PRECEDENCE - 10)
@RequiredArgsConstructor
public class AiSubscriptionPlanFilter extends OncePerRequestFilter {

    private final AiSubscriptionGuard aiSubscriptionGuard;
    private final ObjectMapper objectMapper;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        if (path == null) {
            return true;
        }
        return !isProtectedAiPath(path);
    }

    static boolean isProtectedAiPath(String path) {
        return path.startsWith("/api/v1/ai/forecasting")
                || path.startsWith("/api/v1/ai/forecast")
                || path.startsWith("/api/v1/ai/recommendations");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            aiSubscriptionGuard.assertProPlanEnabled();
            filterChain.doFilter(request, response);
        } catch (FeatureNotAvailableException ex) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            objectMapper.writeValue(response.getWriter(),
                    ApiResponse.error(ex.getMessage(), ex.getErrorCode()));
        }
    }
}

