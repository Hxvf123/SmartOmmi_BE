package com.smartomni.integration.dto.tiktok;

import lombok.Data;

/**
 * Request body cho API kết nối TikTok Shop (POST /api/v1/tiktok/connect).
 */
@Data
public class TiktokConnectRequest {
    private String code;
    private String state;
    private String error;
    private String shopId;
    private String shopName;
}

