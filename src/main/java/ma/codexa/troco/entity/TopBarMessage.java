package ma.codexa.troco.entity;

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

    /** Durée d'affichage de ce message avant passage au suivant (rotation), en secondes — comme autoCloseSeconds des promo modals */
    @Column(name = "display_duration_seconds", nullable = false)
    private Integer displayDurationSeconds = 7;

    public TopBarMessage(String message, Integer displayOrder, Boolean isActive) {
        this.message = message;
        this.displayOrder = displayOrder;
        this.isActive = isActive;
        this.displayDurationSeconds = 7;
    }

    @PrePersist
    @PreUpdate
    private void ensureDefaultsBeforePersist() {
        if (displayDurationSeconds == null) {
            displayDurationSeconds = 7;
        }
        if (isActive == null) {
            isActive = true;
        }
    }
}
