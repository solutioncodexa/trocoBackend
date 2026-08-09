package ma.codexa.troco.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    /** SUPER_ADMIN | ADMIN | STAFF | CUSTOMER */
    @Column(nullable = false)
    private String role;

    /** Null pour SUPER_ADMIN (plateforme). Obligatoire pour ADMIN/STAFF/CUSTOMER. */
    @Column(name = "fournisseur_id")
    private Long fournisseurId;

    @Column(name = "full_name")
    private String fullName;

    @Column(nullable = false)
    private boolean active = true;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_permissions", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "permission_code", nullable = false, length = 80)
    private Set<String> permissionCodes = new HashSet<>();

    /**
     * Préférences UI JSON (guide admin, etc.).
     * Ex. {@code {"adminGuideVersion":1,"adminGuideCompleted":true}}
     */
    @Column(name = "ui_preferences_json", columnDefinition = "TEXT")
    private String uiPreferencesJson;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (permissionCodes == null) {
            permissionCodes = new HashSet<>();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
