package ma.codexa.troco.dto;

import java.time.LocalDateTime;

/** Liste admin pages — sans blocs ni SEO lourds. */
public record StorePageListItemDTO(
        Long id,
        String title,
        String titleAr,
        String slug,
        boolean isHome,
        boolean showInNav,
        boolean published,
        Integer sortOrder,
        boolean currentlyLive,
        String abVariant,
        LocalDateTime publishAt,
        LocalDateTime unpublishAt,
        int blockCount
) {}
