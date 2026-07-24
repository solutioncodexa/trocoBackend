package ma.codexa.troco.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "home_hero_settings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class HomeHeroSettings {

    @Id
    private Long id = 1L;

    /** Première image (rétrocompatibilité). */
    @Column(name = "image_url", length = 1024)
    private String imageUrl;

    /** Liste JSON des URLs d'images du hero, dans l'ordre d'affichage. */
    @Column(name = "image_urls", columnDefinition = "TEXT")
    private String imageUrlsJson;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    protected void touch() {
        updatedAt = LocalDateTime.now();
    }
}
