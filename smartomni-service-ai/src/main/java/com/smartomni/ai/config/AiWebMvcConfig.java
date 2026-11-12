package com.smartomni.ai.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Đăng ký Interceptor khóa tính năng AI cho các gói Free.
 */
@Configuration
@RequiredArgsConstructor
public class AiWebMvcConfig implements WebMvcConfigurer {

    private final AiPlanInterceptor aiPlanInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(aiPlanInterceptor)
                .addPathPatterns(
                        "/api/v1/ai/forecasting/**",
                        "/api/v1/ai/forecast/**",
                        "/api/v1/ai/recommendations/**"
                );
    }
}

