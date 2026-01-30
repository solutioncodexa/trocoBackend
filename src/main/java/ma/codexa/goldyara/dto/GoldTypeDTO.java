package ma.codexa.goldyara.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GoldTypeDTO {

    private Long id;

    @NotBlank(message = "Le nom est obligatoire")
    @Size(max = 100)
    private String name;

    @NotBlank(message = "Le code est obligatoire")
    @Size(max = 50)
    private String code;

    private Integer sortOrder;
}
