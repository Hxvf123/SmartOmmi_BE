package com.smartomni.integration.service;

import com.smartomni.integration.client.MarketplaceClient;
import com.smartomni.integration.client.ShopeeClient;
import com.smartomni.integration.client.TiktokShopClient;
import com.smartomni.integration.dto.MarketplaceProductDto;
import com.smartomni.integration.entity.MarketplaceConnection;
import com.smartomni.integration.repository.MarketplaceConnectionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * UC-41: He thong tu dong fetch & nhap san pham tu san TMDT.
 * FR-082 (goi GetProductList), FR-083 (anh xa & tao san pham), FR-084 (phan trang),
 * FR-085 (upsert theo SKU), FR-086 (dung khi vuot gioi han SKU), FR-087 (log loi khong gian doan batch).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductSyncService {

    private static final int PAGE_SIZE = 50;

    private final MarketplaceConnectionRepository connectionRepository;
    private final AesEncryptionService encryptionService;
    private final ShopeeClient shopeeClient;
    private final TiktokShopClient tiktokShopClient;
    // private final CatalogServiceClient catalogServiceClient; // TODO Feign client -> smartomni-service-catalog
    // private final TenantServiceClient tenantServiceClient;   // TODO kiem tra gioi han SKU (FR-086)

    public void importAllProducts(Long tenantId, MarketplaceConnection.Platform platform) {
        MarketplaceConnection connection = connectionRepository.findByTenantIdAndPlatform(tenantId, platform)
                .orElseThrow(() -> new IllegalStateException("Chua ket noi voi " + platform));

        MarketplaceClient client = resolveClient(platform);
        String appSecret = encryptionService.decrypt(connection.getAppSecretEncrypted());

        int page = 0;
        int successCount = 0;
        int failCount = 0;

        while (true) {
            // FR-084: phan trang de tranh timeout voi danh sach lon
            List<MarketplaceProductDto> products = client.getProductList(connection.getAppKey(), appSecret, page, PAGE_SIZE);
            if (products.isEmpty()) break;

            for (MarketplaceProductDto product : products) {
                try {
                    // TODO FR-086: kiem tra gioi han SKU truoc khi tao - neu vuot, dung va thong bao nang cap goi
                    // if (!tenantServiceClient.canAddMoreSkus(tenantId, product.getVariants().size())) {
                    //     log.warn("Da dat gioi han SKU cho tenant={}, dung dong bo", tenantId);
                    //     return;
                    // }

                    // FR-083, FR-085: upsert san pham + SKU vao Catalog Service (qua Feign)
                    // catalogServiceClient.upsertProductFromMarketplace(tenantId, product);
                    successCount++;
                } catch (Exception ex) {
                    // FR-087: ghi log loi, KHONG lam gian doan ca batch
                    log.error("Loi nhap san pham '{}' cho tenant={}: {}", product.getName(), tenantId, ex.getMessage());
                    failCount++;
                }
            }
            page++;
        }

        log.info("Hoan tat dong bo san pham tu {} cho tenant={}: {} thanh cong, {} that bai",
                platform, tenantId, successCount, failCount);
        // TODO: luu bao cao ket qua (successCount/failCount) de hien thi cho Admin/Manager
    }

    private MarketplaceClient resolveClient(MarketplaceConnection.Platform platform) {
        return switch (platform) {
            case SHOPEE -> shopeeClient;
            case TIKTOK_SHOP -> tiktokShopClient;
        };
    }
}
