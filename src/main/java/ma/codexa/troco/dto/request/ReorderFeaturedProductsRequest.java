package ma.codexa.troco.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class ReorderFeaturedProductsRequest {
    
    @NotNull(message = "La section est requise")
    private String section; // 'heritage' or 'sur-mesure'
    
    @NotEmpty(message = "La liste des IDs de produits ne peut pas être vide")
    private List<Long> productIds;
}
