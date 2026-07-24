package ma.codexa.troco.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class CreateFeaturedProductRequest {
    
    @NotNull(message = "L'ID du produit est requis")
    private Long productId;
    
    @NotNull(message = "La section est requise")
    private String section; // 'heritage' or 'sur-mesure'
    
    private String title;
    
    private String description;
    
    private String imageUrl;
    
    @Min(value = 0, message = "L'ordre d'affichage doit être positif")
    private Integer displayOrder = 0;
    
    private Boolean isActive = true;
}
