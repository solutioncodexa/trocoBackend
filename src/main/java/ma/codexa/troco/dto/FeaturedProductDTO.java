package ma.codexa.troco.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FeaturedProductDTO {
    private Long id;
    private Long productId;
    private String section;
    private String title;
    private String description;
    private String imageUrl;
    private Integer displayOrder;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Product information
    private ProductInfoDTO product;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductInfoDTO {
        private Long id;
        private String name;
        private String description;
        private Double price;
        private String imageUrl;
        private String category;
        private String marque;
        private Double weight;
        private Boolean isActive;
    }
}
