package ma.codexa.troco.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

/** Corps des requêtes de l'import Instagram. */
public final class InstagramRequests {

    private InstagramRequests() {}

    public record Links(@NotEmpty @Size(max = 20, message = "20 liens maximum par envoi") List<@Size(max = 500) String> urls) {}

    /** Champs modifiables à la relecture ; un champ absent reste inchangé. */
    public record DraftUpdate(
            @Size(max = 200) String name,
            @Size(max = 10000) String description,
            Double price,
            @Max(100000) Integer stock,
            String categorySlug,
            /** Ordre et sélection finale des images (la première devient la principale). */
            List<String> images
    ) {}

    public record Publish(
            @NotEmpty @Size(max = 50) List<Long> ids,
            /** Catégorie appliquée aux brouillons qui n'en ont pas encore. */
            String defaultCategorySlug
    ) {}
}
