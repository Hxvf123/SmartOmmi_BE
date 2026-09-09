package com.smartomni.integration.service;

import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

/**
 * UC-32/FR-058: Xac thuc chu ky so (HMAC/SHA256) cua moi Webhook nhan tu san TMDT,
 * dam bao request thuc su den tu Shopee/TikTok Shop chu khong phai gia mao.
 */
@Service
public class WebhookSignatureValidator {

    private static final String HMAC_ALGO = "HmacSHA256";

    public boolean isValid(String rawPayload, String receivedSignature, String appSecretDecrypted) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGO);
            mac.init(new SecretKeySpec(appSecretDecrypted.getBytes(StandardCharsets.UTF_8), HMAC_ALGO));
            byte[] hash = mac.doFinal(rawPayload.getBytes(StandardCharsets.UTF_8));
            String computedSignature = HexFormat.of().formatHex(hash);
            return computedSignature.equalsIgnoreCase(receivedSignature);
        } catch (Exception e) {
            return false;
        }
    }
}
