package com.smartomni.order.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

/**
 * DTO chuan hoa payload don hang tu Webhook Shopee/TikTok Shop (sau khi da
 * duoc chuan hoa boi smartomni-service-integration truoc khi day vao Queue).
 * UC-32/UC-33.
 */
@Getter
@Setter
public class WebhookOrderPayload {
    private Long tenantId;
    private String platform; // SHOPEE | TIKTOK_SHOP
    private String platformOrderId;
    private String customerName;
    private String customerPhone;
    private String shippingAddress;
    private BigDecimal totalAmount;
    private List<OrderItemPayload> items;

    @Getter
    @Setter
    public static class OrderItemPayload {
        private Long skuId;
        private Integer quantity;
        private BigDecimal unitPrice;
    }
}
