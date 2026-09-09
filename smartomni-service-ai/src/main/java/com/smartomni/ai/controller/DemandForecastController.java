package com.smartomni.ai.controller;

import com.smartomni.ai.entity.DemandForecast;
import com.smartomni.ai.service.DemandForecastService;
import com.smartomni.common.dto.ApiResponse;
import com.smartomni.common.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** UC-14/UC-35: Du bao nhu cau hang hoa & canh bao ton kho. */
@RestController
@RequestMapping("/api/ai/forecast")
@RequiredArgsConstructor
public class DemandForecastController {

    private final DemandForecastService demandForecastService;

    @PostMapping("/run")
    public ApiResponse<Void> runForecast() {
        demandForecastService.runForecastForTenant(TenantContext.getTenantId());
        return ApiResponse.success("Da chay du bao nhu cau", null);
    }

    @GetMapping("/{skuId}")
    public ApiResponse<List<DemandForecast>> getForecasts(@PathVariable Long skuId) {
        return ApiResponse.success(demandForecastService.getForecastsForSku(TenantContext.getTenantId(), skuId));
    }
}
