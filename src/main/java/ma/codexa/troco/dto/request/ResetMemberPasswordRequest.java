package ma.codexa.troco.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ResetMemberPasswordRequest {

    @NotBlank
    @Size(min = 6, max = 100)
    private String password;
}
