package ma.codexa.troco.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SocialNetworkDTO {
    private Long id;
    private String networkKey;
    private String label;
    private String url;
    private Boolean enabled;
    private Integer displayOrder;
}
