package ma.codexa.troco.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import ma.codexa.troco.tenant.TenantScoped;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "carts")
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Cart extends TenantScoped {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "session_id")
    private String sessionId;

    @OneToMany(mappedBy = "cart", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnoreProperties("cart")
    private List<CartItem> items = new ArrayList<>();

    @Column(name = "total_amount")
    private Double totalAmount = 0.0;

    public void calculateTotal() {
        this.totalAmount = items.stream()
            .mapToDouble(CartItem::getSubtotal)
            .sum();
    }
}
