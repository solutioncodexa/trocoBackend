package ma.codexa.troco.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AiCopyRequest {
    /** seo_title | seo_description | hero | faq | cta */
    @NotBlank
    @Size(max = 40)
    private String kind;

    @NotBlank
    @Size(max = 200)
    private String topic;

    @Size(max = 120)
    private String storeName;

    @Size(max = 80)
    private String tone; // pro | friendly | luxury
}
