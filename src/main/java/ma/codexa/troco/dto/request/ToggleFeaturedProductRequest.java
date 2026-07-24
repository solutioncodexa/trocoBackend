package ma.codexa.troco.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ToggleFeaturedProductRequest {
    
    @NotNull(message = "Le statut isActive est requis")
    private Boolean isActive;
}
