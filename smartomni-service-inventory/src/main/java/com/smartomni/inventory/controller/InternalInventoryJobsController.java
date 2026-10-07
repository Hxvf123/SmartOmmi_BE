package com.smartomni.inventory.controller;

import com.smartomni.inventory.scheduler.OutboxPublisherScheduler;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/jobs")
public class InternalInventoryJobsController {
    private final OutboxPublisherScheduler scheduler;

    @PostMapping("/publish-outbox")
    public ResponseEntity<Void> publishOutbox() {
        scheduler.publishPendingEvents();
        return ResponseEntity.noContent().build();
    }
}
