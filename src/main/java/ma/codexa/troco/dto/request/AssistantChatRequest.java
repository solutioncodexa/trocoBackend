package ma.codexa.troco.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record AssistantChatRequest(
        @NotEmpty @Size(max = 40) List<@Valid Message> messages,
        /** Écran admin affiché (ex. /admin/reglages) — aide le modèle à répondre dans le contexte. */
        @Size(max = 120) String route,
        /** Langue de l'interface admin : fr | en | ar (défaut fr). */
        @Size(max = 8) String locale
) {
    public record Message(
            @NotBlank @Size(max = 20) String role,
            @NotBlank @Size(max = 4000) String content
    ) {
    }
}
