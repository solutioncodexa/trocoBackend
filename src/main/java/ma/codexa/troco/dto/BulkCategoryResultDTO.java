package ma.codexa.troco.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BulkCategoryResultDTO {
    private int successCount;
    private int failureCount;
    private java.util.List<String> errors;
}
