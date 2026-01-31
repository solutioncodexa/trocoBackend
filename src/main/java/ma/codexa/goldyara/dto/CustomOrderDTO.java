package ma.codexa.goldyara.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CustomOrderDTO {
    private String id;
    private String imageUrl;
    private List<String> referenceImageUrls;
    private String description;
    private String type; // ProductType
    private Double weight;
    private String style; // ProductCategory
    private CustomerDTO customer;
    private String status; // 'pending', 'contacted', 'completed'
    private String createdAt;
}
