package ma.codexa.troco.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CategoryDTO {
    private Long id;
    private String name;
    private String slug;
    private String description;
    private Long parentId;
    private String parentName;
    private String heroImageUrl;
    private Boolean showOnHero;
    private Integer heroSortOrder;
    private Long productCount;
    /** false = désactivée (masquée vitrine). */
    private Boolean active;
}
