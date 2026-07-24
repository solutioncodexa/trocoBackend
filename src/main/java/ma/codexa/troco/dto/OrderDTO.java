package ma.codexa.troco.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderDTO {
    private String id;
    private List<CartItemDTO> items;
    private CustomerDTO customer;
    private Double total;
    private String paymentMethod; // 'cash_on_delivery' or 'online'
    private String status; // 'new', 'confirmed', 'delivered', 'cancelled'
    private String createdAt;
    private String promoCode;
    private Double discount;
}
