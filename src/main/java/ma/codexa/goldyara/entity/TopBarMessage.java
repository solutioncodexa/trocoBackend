package ma.codexa.goldyara.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Table(name = "top_bar_messages")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TopBarMessage {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String message;
    
    @Column(nullable = false)
    private Integer displayOrder;
    
    @Column(nullable = false)
    private Boolean isActive = true;
    
    public TopBarMessage(String message, Integer displayOrder, Boolean isActive) {
        this.message = message;
        this.displayOrder = displayOrder;
        this.isActive = isActive;
    }
}
