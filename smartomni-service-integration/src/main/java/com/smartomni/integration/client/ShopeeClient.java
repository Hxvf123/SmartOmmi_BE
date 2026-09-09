package com.smartomni.integration.client;

import com.smartomni.integration.dto.MarketplaceProductDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.util.List;

/**
 * Trien khai MarketplaceClient cho Shopee Open Platform.
 * TODO: thay the stub bang goi that toi Shopee Open API (GetProductList, GetItemBaseInfo,
 * UpdateStock...) su dung WebClient, ky Sign theo tai lieu Shopee (partner_key + HMAC-SHA256).
 * Tham khao: https://open.shopee.com/documents
 */
@Slf4j
@Component
public class ShopeeClient implements MarketplaceClient {

    private final WebClient webClient = WebClient.builder()
            .baseUrl("https://partner.shopeemobile.com")
            .build();

    @Override
    public List<MarketplaceProductDto> getProductList(String appKey, String appSecretDecrypted, int page, int pageSize) {
        log.info("[STUB] Goi Shopee GetProductList - appKey={}, page={}, pageSize={}", appKey, page, pageSize);
        // TODO: goi that qua webClient.get().uri(...).retrieve()...
        return List.of();
    }

    @Override
    public BigDecimal getCurrentPrice(String appKey, String appSecretDecrypted, String platformItemId) {
        log.info("[STUB] Goi Shopee GetItemDetail lay gia - itemId={}", platformItemId);
        return null;
    }

    @Override
    public void updateStock(String appKey, String appSecretDecrypted, String platformItemId, int quantity) {
        log.info("[STUB] Goi Shopee UpdateStock - itemId={}, quantity={}", platformItemId, quantity);
    }
}
