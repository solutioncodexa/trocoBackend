package ma.codexa.troco.dto;

public record StorePageNavDTO(
        Long id,
        String title,
        String titleAr,
        String slug,
        boolean isHome
) {}
