package ma.codexa.troco.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class BulkCategoryIdsRequest {

    @NotEmpty(message = "Sélectionnez au moins une catégorie")
    private List<@NotNull Long> ids;
}
