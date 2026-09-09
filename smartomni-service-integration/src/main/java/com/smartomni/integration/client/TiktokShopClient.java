package com.smartomni.integration.client;

import com.smartomni.integration.dto.MarketplaceProductDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.util.List;

/**
 * Trien khai MarketplaceClient cho TikTok Shop Open API.
 * TODO: thay the stub bang goi that toi TikTok Shop Open API.
 * Tham khao: https://partner.tiktokshop.com/docv2
 */
@Slf4j
@Component
public class TiktokShopClient implements MarketplaceClient {

    private final WebClient webClient = WebClient.builder()
            .baseUrl("https://open-api.tiktokglobalshop.com")
            .build();

    @Override
    public List<MarketplaceProductDto> getProductList(String appKey, String appSecretDecrypted, int page, int pageSize) {
        log.info("[STUB] Goi TikTok Shop GetItemList - appKey={}, page={}, pageSize={}", appKey, page, pageSize);
        return List.of();
    }

    @Override
    public BigDecimal getCurrentPrice(String appKey, String appSecretDecrypted, String platformItemId) {
        log.info("[STUB] Goi TikTok Shop GetProductDetail lay gia - itemId={}", platformItemId);
        return null;
    }

    @Override
    public void updateStock(String appKey, String appSecretDecrypted, String platformItemId, int quantity) {
        log.info("[STUB] Goi TikTok Shop UpdateInventory - itemId={}, quantity={}", platformItemId, quantity);
    }
}
