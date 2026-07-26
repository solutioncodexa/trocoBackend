package ma.codexa.troco.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class UpdateStoreSettingsRequest {

    @Size(max = 200)
    private String siteName;

    private String tagline;
    private String aboutText;

    @Size(max = 2048)
    private String logoUrl;

    @Size(max = 20)
    private String primaryColor;

    @Size(max = 20)
    private String secondaryColor;

    /** Domaine personnalisé ex. boutique.ma (sans protocole) */
    @Size(max = 255)
    private String customDomain;

    @Size(max = 255)
    private String contactEmail;

    @Size(max = 50)
    private String contactPhone;

    @Size(max = 50)
    private String contactWhatsapp;

    @Size(max = 120)
    private String contactCity;

    private BigDecimal freeShippingThreshold;

    @Size(max = 512)
    private String facebookUrl;

    @Size(max = 512)
    private String instagramUrl;

    @Size(max = 512)
    private String tiktokUrl;

    @Size(max = 1024)
    private String faviconUrl;

    private Boolean heroEnabled;
    private Boolean categoriesEnabled;
    private Boolean surMesureEnabled;

    /** classic | minimal | bold | elegant */
    @Size(max = 40)
    private String themeKey;

    @Size(max = 64)
    private String metaPixelId;

    @Size(max = 64)
    private String tiktokPixelId;

    @Size(max = 64)
    private String googleAdsId;

    @Size(max = 64)
    private String googleAnalyticsId;

    private Boolean abandonedCartEnabled;
    private Integer abandonedCartDelayMinutes;
    private String whatsappOrderTemplate;
}
