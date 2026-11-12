package com.smartomni.ai.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Interceptor chặn request gói FREE truy cập các endpoint AI trước khi vào Controller.
 */
@Component
@RequiredArgsConstructor
public class AiPlanInterceptor implements HandlerInterceptor {

    private final AiSubscriptionGuard aiSubscriptionGuard;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        aiSubscriptionGuard.assertProPlanEnabled();
        return true;
    }
}

