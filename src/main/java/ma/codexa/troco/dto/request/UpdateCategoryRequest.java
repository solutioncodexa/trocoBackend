package ma.codexa.troco.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateCategoryRequest {

    @NotBlank(message = "Le nom est obligatoire")
    @Size(max = 200)
    private String name;

    @NotBlank(message = "Le slug est obligatoire")
    @Size(max = 200)
    private String slug;

    private String description;

    /** null = racine ; omit vs clear handled via clearParent */
    private Long parentId;

    private Boolean clearParent;
}
