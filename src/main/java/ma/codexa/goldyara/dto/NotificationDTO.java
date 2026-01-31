package ma.codexa.goldyara.dto;

import java.time.LocalDateTime;

public record NotificationDTO(
        Long id,
        String type,
        String title,
        String message,
        Long referenceId,
        boolean read,
        LocalDateTime createdAt
) {}
