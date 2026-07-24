package ma.codexa.troco.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CustomOrderStatsDTO {
    private long total;
    private long pending;
    private long contacted;
    private long completed;
}
