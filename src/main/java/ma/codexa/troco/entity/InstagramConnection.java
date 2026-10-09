package ma.codexa.troco.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ma.codexa.troco.tenant.TenantScoped;

import java.time.LocalDateTime;

/** Compte Instagram autorisé par une boutique ; le jeton d'accès est chiffré. */
@Entity
@Table(name = "instagram_connections")
@Getter
@Setter
@NoArgsConstructor
public class InstagramConnection extends TenantScoped {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ig_user_id", nullable = false, length = 40)
    private String igUserId;

    @Column(length = 100)
    private String username;

    @Column(name = "access_token", nullable = false, columnDefinition = "TEXT")
    private String accessToken;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "refreshed_at", nullable = false)
    private LocalDateTime refreshedAt = LocalDateTime.now();

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}
