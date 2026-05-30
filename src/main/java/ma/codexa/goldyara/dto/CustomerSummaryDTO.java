package ma.codexa.goldyara.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Client allégé pour listes admin (sans adresse ni email).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CustomerSummaryDTO {
    private String fullName;
    private String phone;
    /** Présent uniquement pour les listes de commandes */
    private String city;
}
