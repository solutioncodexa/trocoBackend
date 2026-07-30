package ma.codexa.troco.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import ma.codexa.troco.tenant.TenantScoped;

@Entity
@Table(name = "top_bar_messages")
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
public class TopBarMessage extends TenantScoped {
    
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

    /** Chemins cibles (JSON array ou lignes), vide = toutes les pages. */
    @Column(name = "target_paths", columnDefinition = "TEXT")
    private String targetPaths;

    /** Couleur de fond (hex). Null/vide = dégradé thème. */
    @Column(name = "background_color", length = 32)
    private String backgroundColor;

    /** Couleur du texte (hex). Null/vide = texte thème. */
    @Column(name = "text_color", length = 32)
    private String textColor;

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
        if (getFournisseurId() == null) {
            Long fid = ma.codexa.troco.tenant.TenantContext.getFournisseurId();
            if (fid != null) {
                setFournisseurId(fid);
            }
        }
    }
}
