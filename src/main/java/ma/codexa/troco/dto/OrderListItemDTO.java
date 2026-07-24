package ma.codexa.troco.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Commande allégée pour listes paginées admin (sans articles détaillés ni produits complets).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OrderListItemDTO {
    private String id;
    private CustomerSummaryDTO customer;
    private Double total;
    private String status;
    private String createdAt;
    private Integer itemCount;
    private String previewProductName;
    private String previewProductImage;
}
