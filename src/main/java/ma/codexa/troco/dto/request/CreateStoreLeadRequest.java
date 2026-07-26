package ma.codexa.troco.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateStoreLeadRequest {
    @NotBlank
    @Size(max = 40)
    private String leadType; // newsletter | lead | devis

    @Size(max = 200)
    private String fullName;

    @Size(max = 255)
    private String email;

    @Size(max = 50)
    private String phone;

    private String message;

    @Size(max = 255)
    private String sourcePath;
}
