package ma.codexa.troco.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StockSettingsDTO {
    private Integer defaultSafetyStock;
    private Integer expiryAlertDays;
    private Boolean alertsEnabled;
    private Boolean lowStockAlertsEnabled;
    private Boolean expiryAlertsEnabled;
}
