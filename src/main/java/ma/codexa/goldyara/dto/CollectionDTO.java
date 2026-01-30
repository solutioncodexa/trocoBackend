package ma.codexa.goldyara.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CollectionDTO {
    private String id;
    private String name;
    private String slug;
    private String description;
    private Boolean isActive;
    private String createdAt;
}
