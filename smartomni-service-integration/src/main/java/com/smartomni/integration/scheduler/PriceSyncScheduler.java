package com.smartomni.integration.scheduler;

import com.smartomni.integration.client.MarketplaceClient;
import com.smartomni.integration.client.ShopeeClient;
import com.smartomni.integration.client.TiktokShopClient;
import com.smartomni.integration.entity.MarketplaceConnection;
import com.smartomni.integration.repository.MarketplaceConnectionRepository;
import com.smartomni.integration.service.AesEncryptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;
import com.smartomni.common.tenant.TenantContext;

import java.math.BigDecimal;

/**
 * UC-43: He thong tu dong fetch & cap nhat gia san pham theo lich.
 * FR-090 (goi API lay gia theo lich), FR-091 (luu gia rieng theo kenh),
 * FR-092 (canh bao chenh lech bat thuong), FR-093 (log lich su thay doi gia).
 *
 * TODO: thay vong lap don gian nay bang truy van PriceSyncConfig (Catalog Service)
 * de biet chinh xac SKU/Tenant nao da bat dong bo gia va tan suat tuong ung,
 * thay vi quet toan bo MarketplaceConnection moi 15 phut nhu hien tai.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PriceSyncScheduler {

    private static final BigDecimal ANOMALY_THRESHOLD_PERCENT = BigDecimal.valueOf(50); // FR-092

    private final MarketplaceConnectionRepository connectionRepository;
    private final AesEncryptionService encryptionService;
    private final ShopeeClient shopeeClient;
    private final TiktokShopClient tiktokShopClient;
    private final TransactionTemplate transactionTemplate;
    // private final CatalogServiceClient catalogServiceClient; // TODO: lay danh sach SKU da bat dong bo gia + gia hien tai

    public void syncPricesForAllTenants() {
        log.debug("Bat dau chu ky dong bo gia tu dong (UC-43)");

        java.util.List<MarketplaceConnection> connections;
        try (var scope = TenantContext.openScope(null, "price_sync_worker", null)) {
            connections = transactionTemplate.execute(status -> connectionRepository
                    .findByStatus(MarketplaceConnection.ConnectionStatus.CONNECTED));
        }
        for (MarketplaceConnection connection : connections) {
            try (var scope = TenantContext.openScope(connection.getTenantId(), "manager", null)) {
                syncPricesForConnection(connection);
            } catch (Exception ex) {
                // FR-090 (A1): retry se duoc xu ly o lan chay ke tiep, giu gia cu
                log.warn("Dong bo gia that bai cho tenant={}, platform={}: {}",
                        connection.getTenantId(), connection.getPlatform(), ex.getMessage());
            }
        }
    }

    private void syncPricesForConnection(MarketplaceConnection connection) {
        MarketplaceClient client = connection.getPlatform() == MarketplaceConnection.Platform.SHOPEE
                ? shopeeClient : tiktokShopClient;
        String appSecret = encryptionService.decrypt(connection.getAppSecretEncrypted());

        // TODO: thay bang danh sach that lay tu Catalog Service (PriceSyncConfig + ProductMarketplaceLink)
        // for (var link : catalogServiceClient.getSkusWithPriceSyncEnabled(connection.getTenantId(), connection.getPlatform())) {
        //     BigDecimal newPrice = client.getCurrentPrice(connection.getAppKey(), appSecret, link.getPlatformItemId());
        //     BigDecimal oldPrice = link.getPlatformPrice();
        //     boolean isAnomaly = isAnomalyChange(oldPrice, newPrice);
        //     catalogServiceClient.updatePlatformPrice(link.getId(), newPrice, isAnomaly); // FR-091, FR-093
        //     if (isAnomaly) {
        //         // FR-092: dua vao hang doi cho Manager xac nhan thay vi ap dung ngay
        //     }
        // }
    }

    /** FR-092: phat hien chenh lech gia bat thuong (vd giam hon 50%). */
    private boolean isAnomalyChange(BigDecimal oldPrice, BigDecimal newPrice) {
        if (oldPrice == null || oldPrice.compareTo(BigDecimal.ZERO) == 0) return false;
        BigDecimal changePercent = oldPrice.subtract(newPrice).abs()
                .divide(oldPrice, 4, java.math.RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
        return changePercent.compareTo(ANOMALY_THRESHOLD_PERCENT) > 0;
    }
}
