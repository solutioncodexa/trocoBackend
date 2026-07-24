package ma.codexa.troco.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Demande de personnalisation allégée pour listes paginées admin.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CustomOrderListItemDTO {
    private String id;
    private String imageUrl;
    private String description;
    private String type;
    private Double weight;
    private String style;
    private CustomerSummaryDTO customer;
    private String status;
    private String createdAt;
}
