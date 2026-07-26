package ma.codexa.troco.dto;

import java.time.LocalDateTime;

public record StoreLeadDTO(
        Long id,
        String leadType,
        String fullName,
        String email,
        String phone,
        String message,
        String sourcePath,
        LocalDateTime createdAt
) {}
