package ma.codexa.troco.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Data
public class ReplaceStorePageBlocksRequest {

    @NotNull
    @Valid
    private List<BlockInput> blocks = new ArrayList<>();

    /** Libellé optionnel pour la version sauvegardée. */
    private String versionLabel;

    @Data
    public static class BlockInput {
        @NotBlank
        private String type;

        private Integer sortOrder;
        private Map<String, Object> config;
        private Map<String, Object> configAr;
        private Boolean visibleMobile;
        private Boolean visibleDesktop;
    }
}
