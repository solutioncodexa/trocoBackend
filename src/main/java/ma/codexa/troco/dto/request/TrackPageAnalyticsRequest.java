package ma.codexa.troco.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.Map;

@Data
public class TrackPageAnalyticsRequest {
    private Long pageId;

    @NotBlank
    private String eventType; // view | cta_click

    private String path;
    private Map<String, Object> meta;
}
