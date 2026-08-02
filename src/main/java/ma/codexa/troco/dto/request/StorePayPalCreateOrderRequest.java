package ma.codexa.troco.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class StorePayPalCreateOrderRequest {
    @NotNull
    @DecimalMin("0.01")
    private BigDecimal amount;

    @NotBlank
    private String currency;

    private String description;
}
