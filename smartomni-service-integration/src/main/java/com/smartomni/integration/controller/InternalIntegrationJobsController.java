package com.smartomni.integration.controller;

import com.smartomni.integration.scheduler.PriceSyncScheduler;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/jobs")
public class InternalIntegrationJobsController {
    private final PriceSyncScheduler scheduler;

    @PostMapping("/sync-prices")
    public ResponseEntity<Void> syncPrices() {
        scheduler.syncPricesForAllTenants();
        return ResponseEntity.noContent().build();
    }
}
