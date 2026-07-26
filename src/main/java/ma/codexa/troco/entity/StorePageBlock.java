package ma.codexa.troco.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import ma.codexa.troco.tenant.TenantScoped;

import java.time.LocalDateTime;

@Entity
@Table(name = "store_page_blocks")
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
public class StorePageBlock extends TenantScoped {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "page_id", nullable = false)
    private StorePage page;

    @Column(name = "block_type", nullable = false, length = 60)
    private String blockType;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;

    @Column(name = "config_json", nullable = false, columnDefinition = "TEXT")
    private String configJson = "{}";

    @Column(name = "config_json_ar", columnDefinition = "TEXT")
    private String configJsonAr;

    @Column(name = "visible_mobile", nullable = false)
    private Boolean visibleMobile = true;

    @Column(name = "visible_desktop", nullable = false)
    private Boolean visibleDesktop = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (sortOrder == null) sortOrder = 0;
        if (configJson == null || configJson.isBlank()) configJson = "{}";
        if (visibleMobile == null) visibleMobile = true;
        if (visibleDesktop == null) visibleDesktop = true;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
