package com.smartomni.integration.entity;

import com.smartomni.common.entity.BaseTenantEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * UC-18: Cau hinh ket noi san TMDT (App Key/Secret ma hoa AES-256).
 * UC-40: Co "autoImportProducts" - luu lua chon cua Admin ngay sau khi ket noi.
 */
@Getter
@Setter
@Entity
@Table(name = "marketplace_connections", uniqueConstraints = @UniqueConstraint(columnNames = {"tenant_id", "platform"}))
public class MarketplaceConnection extends BaseTenantEntity {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Platform platform;

    @Column(name = "app_key")
    private String appKey;

    @Column(name = "app_secret_encrypted")
    private String appSecretEncrypted; // FR-038: ma hoa AES-256 truoc khi luu

    @Column(name = "webhook_url")
    private String webhookUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ConnectionStatus status = ConnectionStatus.CONNECTED;

    @Column(name = "auto_import_products")
    private boolean autoImportProducts = false; // FR-080, FR-081

    public enum Platform { SHOPEE, TIKTOK_SHOP }
    public enum ConnectionStatus { CONNECTED, ERROR, DISCONNECTED }
}
