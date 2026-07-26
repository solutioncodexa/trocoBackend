package ma.codexa.troco.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UpsertBlogPostRequest {
    @NotBlank
    @Size(max = 250)
    private String title;

    @Size(max = 160)
    private String slug;

    @Size(max = 500)
    private String excerpt;

    @NotBlank
    private String content;

    private String coverUrl;
    private String seoTitle;
    private String seoDescription;
    private String lang;
    private Boolean published;
    private LocalDateTime publishAt;
}
