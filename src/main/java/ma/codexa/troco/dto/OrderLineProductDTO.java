package ma.codexa.troco.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Produit allégé pour lignes de commande / panier API.
 * Le détail complet reste sur GET /products/{id}.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderLineProductDTO {
    private String id;
    private String name;
    private Double price;
    /** Une seule image (aperçu). */
    private List<String> images;
    private Double weight;
    private String sku;
}
