package ma.codexa.troco.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UpsertStorePageRequest {
    @NotBlank
    @Size(max = 200)
    private String title;

    @Size(max = 200)
    private String titleAr;

    @Size(max = 120)
    private String slug;

    private Boolean isHome;
    private Boolean showInNav;
    private Boolean published;
    private Integer sortOrder;

    @Size(max = 200)
    private String seoTitle;

    @Size(max = 500)
    private String seoDescription;

    @Size(max = 1024)
    private String ogImageUrl;

    @Size(max = 200)
    private String seoTitleAr;

    @Size(max = 500)
    private String seoDescriptionAr;

    private LocalDateTime publishAt;
    private LocalDateTime unpublishAt;

    /** true = clear publishAt */
    private Boolean clearPublishAt;
    /** true = clear unpublishAt */
    private Boolean clearUnpublishAt;

    /** Variante A/B accueil : A, B, ou vide pour désactiver */
    @Size(max = 1)
    private String abVariant;

    private Boolean clearAbVariant;
}
