package ma.codexa.troco.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class UpdateMemberRequest {

    @Size(max = 255)
    private String fullName;

    private Boolean active;

    private List<String> permissions = new ArrayList<>();
}
