package ma.codexa.troco.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import ma.codexa.troco.tenant.TenantScoped;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "store_settings")
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
public class StoreSettings extends TenantScoped {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "site_name", length = 200)
    private String siteName;

    @Column(columnDefinition = "TEXT")
    private String tagline;

    @Column(name = "about_text", columnDefinition = "TEXT")
    private String aboutText;

    @Column(name = "contact_email", length = 255)
    private String contactEmail;

    @Column(name = "contact_phone", length = 50)
    private String contactPhone;

    @Column(name = "contact_whatsapp", length = 50)
    private String contactWhatsapp;

    @Column(name = "contact_city", length = 120)
    private String contactCity;

    @Column(name = "free_shipping_threshold", precision = 12, scale = 2)
    private BigDecimal freeShippingThreshold;

    @Column(name = "facebook_url", length = 512)
    private String facebookUrl;

    @Column(name = "instagram_url", length = 512)
    private String instagramUrl;

    @Column(name = "tiktok_url", length = 512)
    private String tiktokUrl;

    @Column(name = "favicon_url", length = 1024)
    private String faviconUrl;

    @Column(name = "hero_enabled", nullable = false)
    private boolean heroEnabled = true;

    @Column(name = "categories_enabled", nullable = false)
    private boolean categoriesEnabled = true;

    @Column(name = "sur_mesure_enabled", nullable = false)
    private boolean surMesureEnabled = true;

    /** classic | minimal | bold | elegant */
    @Column(name = "theme_key", nullable = false, length = 40)
    private String themeKey = "classic";

    /** display_sans | editorial_serif | modern_mono */
    @Column(name = "font_pair", nullable = false, length = 40)
    private String fontPair = "display_sans";

    /** sharp | soft | round */
    @Column(name = "radius_preset", nullable = false, length = 20)
    private String radiusPreset = "soft";

    /** Boutons / cards / hero / header / footer — JSON normalisé. */
    @Column(name = "appearance_json", columnDefinition = "TEXT")
    private String appearanceJson;

    /** Snapshots look par themeKey — JSON { classic: {...}, minimal: {...} }. */
    @Column(name = "theme_presets_json", columnDefinition = "TEXT")
    private String themePresetsJson;

    @Column(name = "meta_pixel_id", length = 64)
    private String metaPixelId;

    @Column(name = "tiktok_pixel_id", length = 64)
    private String tiktokPixelId;

    @Column(name = "google_ads_id", length = 64)
    private String googleAdsId;

    @Column(name = "google_analytics_id", length = 64)
    private String googleAnalyticsId;

    @Column(name = "abandoned_cart_enabled", nullable = false, columnDefinition = "boolean default true")
    private boolean abandonedCartEnabled = true;

    @Column(name = "abandoned_cart_delay_minutes", nullable = false, columnDefinition = "integer default 60")
    private Integer abandonedCartDelayMinutes = 60;

    @Column(name = "whatsapp_order_template", columnDefinition = "TEXT")
    private String whatsappOrderTemplate;

    @Column(name = "default_locale", length = 10)
    private String defaultLocale = "fr";

    @Column(name = "supported_locales", length = 40)
    private String supportedLocales = "fr,ar,en";

    @Column(length = 8)
    private String currency = "MAD";

    @Column(name = "currency_rates_json", columnDefinition = "TEXT")
    private String currencyRatesJson;

    @Column(name = "payment_cod_enabled")
    private Boolean paymentCodEnabled = true;

    @Column(name = "payment_cmi_enabled")
    private Boolean paymentCmiEnabled = false;

    @Column(name = "payment_bnpl_enabled")
    private Boolean paymentBnplEnabled = false;

    @Column(name = "bnpl_provider", length = 40)
    private String bnplProvider = "manual";

    @Column(name = "loyalty_enabled")
    private Boolean loyaltyEnabled = false;

    @Column(name = "loyalty_points_per_mad", precision = 12, scale = 4)
    private BigDecimal loyaltyPointsPerMad = BigDecimal.ONE;

    @Column(name = "loyalty_mad_per_point", precision = 12, scale = 4)
    private BigDecimal loyaltyMadPerPoint = new BigDecimal("0.10");

    @Column(name = "privacy_policy_url", length = 1024)
    private String privacyPolicyUrl;

    @Column(name = "cookie_consent_required")
    private Boolean cookieConsentRequired = true;

    @Column(name = "data_retention_days")
    private Integer dataRetentionDays = 365;

    @Column(name = "cndp_notice_version", length = 40)
    private String cndpNoticeVersion;

    @Column(name = "shipping_default_carrier", length = 40)
    private String shippingDefaultCarrier;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
