package ma.codexa.troco.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.Map;

@Data
public class UpsertGlobalSectionRequest {
    @NotBlank
    @Size(max = 40)
    private String sectionKey;

    private Boolean enabled;
    private Map<String, Object> config;
}
