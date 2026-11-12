package com.smartomni.ai.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartomni.ai.config.AiPlanInterceptor;
import com.smartomni.ai.config.AiSubscriptionGuard;
import com.smartomni.ai.config.AiSubscriptionPlanFilter;
import com.smartomni.ai.service.DemandForecastService;
import com.smartomni.ai.service.RecommendationService;
import com.smartomni.common.exception.GlobalExceptionHandler;
import com.smartomni.common.tenant.TenantContext;
import com.smartomni.common.tenant.TenantPlanResolver;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("AI Service Subscription Plan Guard Tests")
class AiSubscriptionAccessTest {

    private MockMvc mockMvc;
    private DemandForecastService demandForecastService;
    private RecommendationService recommendationService;

    @BeforeEach
    void setUp() {
        demandForecastService = mock(DemandForecastService.class);
        recommendationService = mock(RecommendationService.class);
        AiSubscriptionGuard guard = new AiSubscriptionGuard(new TenantPlanResolver((javax.sql.DataSource) null));
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        AiSubscriptionPlanFilter filter = new AiSubscriptionPlanFilter(guard, objectMapper);
        AiPlanInterceptor interceptor = new AiPlanInterceptor(guard);

        mockMvc = MockMvcBuilders.standaloneSetup(
                        new DemandForecastController(demandForecastService),
                        new RecommendationController(recommendationService))
                .addInterceptors(interceptor)
                .addFilters(filter)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("Gói FREE bị chặn 403 Forbidden khi gọi /api/v1/ai/forecasting/**")
    void testForecasting_FreePlan_Returns403() throws Exception {
        TenantContext.setTenantId(1L);
        TenantContext.setSubscriptionPlan("FREE");

        mockMvc.perform(get("/api/v1/ai/forecasting/10"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("FEATURE_NOT_AVAILABLE"));
    }

    @Test
    @DisplayName("Gói FREE bị chặn 403 Forbidden khi gọi /api/v1/ai/forecast/**")
    void testForecast_FreePlan_Returns403() throws Exception {
        TenantContext.setTenantId(1L);
        TenantContext.setSubscriptionPlan("FREE");

        mockMvc.perform(post("/api/v1/ai/forecast/run"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("FEATURE_NOT_AVAILABLE"));

        verifyNoInteractions(demandForecastService);
    }

    @Test
    @DisplayName("Gói FREE bị chặn 403 Forbidden khi gọi /api/v1/ai/recommendations/**")
    void testRecommendations_FreePlan_Returns403() throws Exception {
        TenantContext.setTenantId(1L);
        TenantContext.setSubscriptionPlan("FREE");

        mockMvc.perform(get("/api/v1/ai/recommendations/100"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("FEATURE_NOT_AVAILABLE"));

        verifyNoInteractions(recommendationService);
    }

    @Test
    @DisplayName("Gói PRO được phép gọi các API forecast và recommendations")
    void testAiEndpoints_ProPlan_Allowed() throws Exception {
        TenantContext.setTenantId(2L);
        TenantContext.setSubscriptionPlan("PRO");

        when(demandForecastService.getForecastsForSku(2L, 10L)).thenReturn(List.of());
        when(recommendationService.getTopRecommendations(2L, 100L)).thenReturn(List.of());

        mockMvc.perform(post("/api/v1/ai/forecast/run"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        mockMvc.perform(get("/api/v1/ai/forecast/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        mockMvc.perform(get("/api/v1/ai/recommendations/100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}

