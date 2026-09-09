package com.smartomni.catalog.service;

import com.smartomni.catalog.dto.CreateProductRequest;
import com.smartomni.catalog.entity.Product;
import com.smartomni.catalog.entity.ProductSku;
import com.smartomni.catalog.repository.ProductRepository;
import com.smartomni.catalog.repository.ProductSkuRepository;
import com.smartomni.common.exception.BusinessException;
import com.smartomni.common.exception.ResourceNotFoundException;
import com.smartomni.common.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * UC-08: Quan ly san pham (Catalog Management) - FR-018,019,020
 * UC-42 (upsert logic dung chung) - FR-085
 */
@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductSkuRepository skuRepository;
    // private final FeatureFlagServiceClient featureFlagClient; // TODO: kiem tra gioi han SKU (FR-020)

    @Transactional
    public Product createProduct(CreateProductRequest request) {
        Long tenantId = TenantContext.getTenantId();

        // FR-019: kiem tra SKU trung trong pham vi Tenant
        for (var skuReq : request.getSkus()) {
            if (skuRepository.existsByTenantIdAndSkuCode(tenantId, skuReq.getSkuCode())) {
                throw new BusinessException("SKU '" + skuReq.getSkuCode() + "' da ton tai trong he thong");
            }
        }

        // TODO FR-020: goi featureFlagClient.assertSkuLimitNotExceeded(tenantId, request.getSkus().size())

        Product product = new Product();
        product.setTenantId(tenantId);
        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setCreatedBy(TenantContext.getCurrentUserId());
        product = productRepository.save(product);

        for (var skuReq : request.getSkus()) {
            ProductSku sku = new ProductSku();
            sku.setTenantId(tenantId);
            sku.setProductId(product.getId());
            sku.setSkuCode(skuReq.getSkuCode());
            sku.setVariantName(skuReq.getVariantName());
            sku.setBasePrice(skuReq.getBasePrice());
            skuRepository.save(sku);
        }

        return product;
    }

    public Product getProductOrThrow(Long productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("San pham khong ton tai: " + productId));
    }

    /**
     * FR-085: co che upsert theo SKU - dung boi ProductSyncService (Integration Service)
     * khi tu dong nhap san pham tu san TMDT (UC-42).
     */
    @Transactional
    public ProductSku upsertSkuFromMarketplace(Long tenantId, Long productId, String skuCode,
                                                String variantName, java.math.BigDecimal price) {
        ProductSku sku = skuRepository.findByTenantIdAndSkuCode(tenantId, skuCode)
                .orElseGet(ProductSku::new);
        sku.setTenantId(tenantId);
        sku.setProductId(productId);
        sku.setSkuCode(skuCode);
        sku.setVariantName(variantName);
        sku.setBasePrice(price);
        return skuRepository.save(sku);
    }

    public List<ProductSku> getSkusByProduct(Long productId) {
        return skuRepository.findByProductId(productId);
    }
}
