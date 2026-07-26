package ma.codexa.troco.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
public class CaptureAbandonedCartRequest {
    @NotBlank
    @Size(max = 80)
    private String sessionKey;

    @Size(max = 255)
    private String customerEmail;

    @Size(max = 50)
    private String customerPhone;

    @Size(max = 200)
    private String customerName;

    @NotNull
    private List<Map<String, Object>> items;

    private BigDecimal cartTotal;
}
