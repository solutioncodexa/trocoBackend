package ma.codexa.troco.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TopBarMessageDTO {
    
    private Long id;
    private String message;
    private Integer displayOrder;
    private Boolean isActive;
    /** Secondes avant d'afficher le message suivant lorsque plusieurs messages sont actifs */
    private Integer displayDurationSeconds;

    public TopBarMessageDTO(String message, Integer displayOrder, Boolean isActive) {
        this.message = message;
        this.displayOrder = displayOrder;
        this.isActive = isActive;
        this.displayDurationSeconds = 7;
    }
}
