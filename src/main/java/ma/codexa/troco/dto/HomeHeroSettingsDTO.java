package ma.codexa.troco.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HomeHeroSettingsDTO {
    /** Première image (compat). */
    private String imageUrl;

    @Builder.Default
    private List<String> imageUrls = new ArrayList<>();
}
