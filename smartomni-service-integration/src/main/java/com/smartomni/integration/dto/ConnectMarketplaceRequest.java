package com.smartomni.integration.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/** UC-18: Cau hinh ket noi san TMDT. */
@Getter
@Setter
public class ConnectMarketplaceRequest {

    @NotBlank
    private String platform; // SHOPEE | TIKTOK_SHOP

    @NotBlank
    private String appKey;

    @NotBlank
    private String appSecret; // se duoc ma hoa AES-256 truoc khi luu (FR-038)

    /** UC-40: Admin chon co tu dong nhap san pham ngay sau khi ket noi hay khong. */
    private boolean autoImportProducts = false;
}
