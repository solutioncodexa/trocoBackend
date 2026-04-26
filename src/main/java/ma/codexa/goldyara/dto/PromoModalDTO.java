package ma.codexa.goldyara.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PromoModalDTO {
    
    private Long id;
    private String title;
    private String description;
    private String imageUrl;
    private String buttonText;
    private String buttonUrl;
    private Integer autoCloseSeconds;
    private Boolean isActive;
    private Integer displayOrder;
}
