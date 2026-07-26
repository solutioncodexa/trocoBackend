package ma.codexa.troco.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateFournisseurRequest {

    @NotBlank
    @Size(max = 200)
    private String name;

    @NotBlank
    @Size(min = 2, max = 100)
    private String slug;

    @NotBlank
    @Email
    private String adminEmail;

    @NotBlank
    @Size(min = 8, max = 100)
    private String adminPassword;

    @Size(max = 120)
    private String adminFullName;

    @Email
    private String email;

    @Size(max = 50)
    private String phone;

    /** Code plan, défaut: basic */
    private String planCode;
}
