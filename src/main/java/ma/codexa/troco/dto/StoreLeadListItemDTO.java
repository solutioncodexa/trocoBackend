package ma.codexa.troco.dto;

import java.time.LocalDateTime;

/** Liste leads — message tronqué (détail via GET /{id}). */
public record StoreLeadListItemDTO(
        Long id,
        String leadType,
        String fullName,
        String email,
        String phone,
        String messagePreview,
        boolean hasMessage,
        String sourcePath,
        LocalDateTime createdAt
) {}
