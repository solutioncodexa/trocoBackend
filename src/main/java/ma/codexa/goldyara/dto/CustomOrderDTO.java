package ma.codexa.goldyara.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CustomOrderDTO {
    private String id;
    private String imageUrl;
    private String description;
    private String type; // ProductType
    private Double weight;
    private String style; // ProductCategory
    private CustomerDTO customer;
    private String status; // 'pending', 'contacted', 'completed'
    private String createdAt;
}
