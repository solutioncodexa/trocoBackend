package ma.codexa.troco.dto;

import java.time.LocalDateTime;
import java.util.List;

public record StorePageDTO(
        Long id,
        String title,
        String titleAr,
        String slug,
        boolean isHome,
        boolean showInNav,
        boolean published,
        Integer sortOrder,
        String seoTitle,
        String seoDescription,
        String ogImageUrl,
        String seoTitleAr,
        String seoDescriptionAr,
        LocalDateTime publishAt,
        LocalDateTime unpublishAt,
        boolean currentlyLive,
        String abVariant,
        String previewToken,
        List<StorePageBlockDTO> blocks
) {}
