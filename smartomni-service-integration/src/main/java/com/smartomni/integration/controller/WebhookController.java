package com.smartomni.integration.controller;

import com.smartomni.integration.entity.MarketplaceConnection;
import com.smartomni.integration.repository.MarketplaceConnectionRepository;
import com.smartomni.integration.service.AesEncryptionService;
import com.smartomni.integration.service.WebhookSignatureValidator;
import com.smartomni.common.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * UC-32: Dong bo don hang qua Webhook (Real-time Sync).
 * FR-057: Nginx route theo path; controller xac thuc tenant_id tu webhook path,
 *         nhung endpoint nay van nhan tenantId truc tiep tu path de co the test
 *         doc lap khong can qua Gateway.
 * FR-058: xac thuc chu ky HMAC/SHA256.
 * FR-059: phan hoi 200 OK trong 1-3 giay - KHONG xu ly dong bo ngay.
 * FR-060: day message vao RabbitMQ kem tenant_id.
 *
 * Luu y: payload tu Shopee/TikTok can duoc chuan hoa (mapping) truoc khi day
 * vao Queue de Order Service (o module khac) co the doc bang WebhookOrderPayload.
 * O day minh hoa don gian bang cach nhan raw payload; buoc mapping chi tiet
 * theo tung san se duoc bo sung khi tich hop API that.
 */
@Slf4j
@RestController
@RequestMapping("/webhook")
@RequiredArgsConstructor
public class WebhookController {

    private static final String ORDER_EXCHANGE = "smartomni.order.exchange";
    private static final String ORDER_ROUTING_KEY = "order.incoming";

    private final MarketplaceConnectionRepository connectionRepository;
    private final AesEncryptionService encryptionService;
    private final WebhookSignatureValidator signatureValidator;
    private final RabbitTemplate rabbitTemplate;

    @PostMapping("/{tenantId}/{platform}")
    public ResponseEntity<String> receiveWebhook(@PathVariable Long tenantId,
                                                  @PathVariable String platform,
                                                  @RequestHeader(value = "X-Signature", required = false) String signature,
                                                  @RequestBody String rawPayload) {

        MarketplaceConnection.Platform platformEnum = MarketplaceConnection.Platform.valueOf(platform.toUpperCase());
        MarketplaceConnection connection;
        if (tenantId <= 0) {
            return ResponseEntity.badRequest().body("Invalid tenant");
        }
        // Resolve only this tenant's connection; authenticate the webhook before enqueueing.
        try (var scope = TenantContext.openScope(tenantId, null, null)) {
            connection = connectionRepository.findByTenantIdAndPlatform(tenantId, platformEnum).orElse(null);
        }

        if (connection == null) {
            log.warn("Webhook nhan cho tenant={} nhung khong tim thay ket noi {}", tenantId, platform);
            return ResponseEntity.badRequest().body("Unknown tenant/platform");
        }

        // FR-058: xac thuc chu ky
        String appSecret = encryptionService.decrypt(connection.getAppSecretEncrypted());
        if (signature == null || !signatureValidator.isValid(rawPayload, signature, appSecret)) {
            log.warn("Chu ky Webhook khong hop le cho tenant={}, platform={}", tenantId, platform);
            return ResponseEntity.status(401).body("Invalid signature");
        }

        // TODO: parse rawPayload theo dung dinh dang cua Shopee/TikTok Shop,
        // chuan hoa thanh WebhookOrderPayload (dinh nghia o smartomni-service-order),
        // roi moi rabbitTemplate.convertAndSend(...) voi doi tuong da chuan hoa.

        // FR-060: day vao RabbitMQ kem tenant_id (minh hoa - can thay bang payload da parse)
        rabbitTemplate.convertAndSend(ORDER_EXCHANGE, ORDER_ROUTING_KEY, rawPayload);

        // FR-059: phan hoi nhanh, khong xu ly dong bo
        return ResponseEntity.ok("OK");
    }
}
