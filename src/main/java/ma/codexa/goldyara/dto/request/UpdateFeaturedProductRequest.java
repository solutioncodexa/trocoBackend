package ma.codexa.goldyara.dto.request;

import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class UpdateFeaturedProductRequest {
    
    private Long productId;
    
    private String section; // 'heritage' or 'sur-mesure'
    
    private String title;
    
    private String description;
    
    private String imageUrl;
    
    @Min(value = 0, message = "L'ordre d'affichage doit être positif")
    private Integer displayOrder;
    
    private Boolean isActive;
}
