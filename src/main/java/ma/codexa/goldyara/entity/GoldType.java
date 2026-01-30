package ma.codexa.goldyara.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "gold_types")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GoldType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    /** Code unique (ex: YELLOW, WHITE, ROSE) utilisé dans Product.goldType */
    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Column(name = "sort_order")
    private Integer sortOrder = 0;
}
