package com.smartomni.integration.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

/** Cau truc san pham chuan hoa tra ve tu API cua Shopee/TikTok Shop (UC-41). */
@Getter
@Setter
public class MarketplaceProductDto {
    private String platformItemId;
    private String name;
    private String description;
    private List<String> imageUrls;
    private List<VariantDto> variants;

    @Getter
    @Setter
    public static class VariantDto {
        private String skuCode;
        private String variantName;
        private BigDecimal price;
    }
}
