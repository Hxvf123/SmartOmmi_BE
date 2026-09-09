package com.smartomni.catalog.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.Valid;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class CreateProductRequest {

    @NotBlank
    private String name;

    private String description;

    @NotEmpty
    @Valid
    private List<SkuRequest> skus;

    @Getter
    @Setter
    public static class SkuRequest {
        @NotBlank
        private String skuCode;
        private String variantName;
        private java.math.BigDecimal basePrice;
    }
}
