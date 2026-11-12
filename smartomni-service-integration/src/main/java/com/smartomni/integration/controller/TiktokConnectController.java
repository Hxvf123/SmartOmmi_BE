package com.smartomni.integration.controller;

import com.smartomni.common.dto.ApiResponse;
import com.smartomni.integration.dto.tiktok.TiktokConnectRequest;
import com.smartomni.integration.service.tiktok.TiktokConnectionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * API kết nối TikTok Shop (POST /api/v1/tiktok/connect) có kiểm soát giới hạn gói cước Free/Pro.
 */
@RestController
@RequestMapping("/api/v1/tiktok")
@RequiredArgsConstructor
public class TiktokConnectController {

    private final TiktokConnectionService tiktokConnectionService;

    @PostMapping("/connect")
    public ApiResponse<Object> connect(@RequestBody(required = false) TiktokConnectRequest request) {
        return ApiResponse.success("Kết nối TikTok Shop thành công", tiktokConnectionService.connect(request));
    }
}

