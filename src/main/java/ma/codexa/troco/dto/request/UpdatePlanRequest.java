package ma.codexa.troco.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Map;

@Data
public class UpdatePlanRequest {

    @Size(max = 50)
    private String code;

    @Size(max = 120)
    private String name;

    private String description;

    @DecimalMin("0.0")
    private BigDecimal priceMad;

    @Size(max = 8)
    private String currency;

    @Size(max = 20)
    private String billingPeriod;

    private Integer maxProducts;
    private Integer maxStaff;
    private Integer maxOrdersPerMonth;
    private Integer maxPixels;
    private Integer storageMb;
    private Boolean customDomain;
    private Boolean active;

    /** Flags fonctionnels (themes, pageBuilder, abTesting, …). */
    private Map<String, Object> features;
}
