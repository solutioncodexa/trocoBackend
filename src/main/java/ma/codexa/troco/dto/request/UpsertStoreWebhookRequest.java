package ma.codexa.troco.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class UpsertStoreWebhookRequest {
    @NotBlank
    @Size(max = 120)
    private String name;

    @NotBlank
    @Size(max = 1024)
    private String targetUrl;

    @Size(max = 120)
    private String secret;

    @NotEmpty
    private List<String> events;

    private Boolean enabled;
}
