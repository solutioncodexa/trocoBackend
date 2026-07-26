package ma.codexa.troco.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class CreateProductReviewRequest {
    @NotNull
    private Long productId;

    @NotBlank
    @Size(max = 120)
    private String authorName;

    @Size(max = 255)
    private String authorEmail;

    @NotNull
    @Min(1)
    @Max(5)
    private Integer rating;

    @Size(max = 200)
    private String title;

    @NotBlank
    @Size(max = 4000)
    private String body;
}
