package ma.codexa.troco.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import ma.codexa.troco.tenant.TenantScoped;

@Entity
@Table(name = "promo_modals")
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
public class PromoModal extends TenantScoped {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String title;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    @Column(name = "image_url", columnDefinition = "TEXT")
    private String imageUrl;
    
    @Column(name = "button_text")
    private String buttonText;
    
    @Column(name = "button_url", columnDefinition = "TEXT")
    private String buttonUrl;
    
    @Column(name = "auto_close_seconds", nullable = false)
    private Integer autoCloseSeconds = 5;
    
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;
    
    @Column(name = "display_order", nullable = false)
    private Integer displayOrder = 1;

    /** Chemins cibles (JSON array ou lignes), vide = toutes les pages. */
    @Column(name = "target_paths", columnDefinition = "TEXT")
    private String targetPaths;
    
    public PromoModal(String title, String description, String imageUrl, String buttonText, String buttonUrl, Integer autoCloseSeconds, Boolean isActive, Integer displayOrder) {
        this.title = title;
        this.description = description;
        this.imageUrl = imageUrl;
        this.buttonText = buttonText;
        this.buttonUrl = buttonUrl;
        this.autoCloseSeconds = autoCloseSeconds;
        this.isActive = isActive;
        this.displayOrder = displayOrder;
    }
}
