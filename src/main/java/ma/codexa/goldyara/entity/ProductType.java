package ma.codexa.goldyara.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "product_types")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    /** Code unique (ex: BRACELET, RING) utilisé dans Product.productType */
    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Column(name = "requires_size", nullable = false)
    private Boolean requiresSize = false;

    /** Tailles possibles séparées par des virgules (ex: 48,50,52 ou 16cm,17cm) */
    @Column(name = "size_options", columnDefinition = "TEXT")
    private String sizeOptions;

    @Column(name = "sort_order")
    private Integer sortOrder = 0;
}
