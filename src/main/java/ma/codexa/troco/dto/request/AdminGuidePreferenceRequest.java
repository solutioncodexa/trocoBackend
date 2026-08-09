package ma.codexa.troco.dto.request;

import jakarta.validation.constraints.NotNull;

/**
 * Marquer le guide première utilisation admin comme terminé / à revoir.
 */
public record AdminGuidePreferenceRequest(
        @NotNull Boolean completed
) {}
