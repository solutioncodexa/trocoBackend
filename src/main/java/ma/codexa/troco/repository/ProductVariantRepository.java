package ma.codexa.troco.repository;

import ma.codexa.troco.entity.ProductVariant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProductVariantRepository extends JpaRepository<ProductVariant, Long> {

    List<ProductVariant> findByProductIdOrderByDisplayOrderAsc(Long productId);

    void deleteByProductId(Long productId);

    /**
     * Variantes du catalogue actif pour un fournisseur.
     * Filtre explicite sur {@code p.fournisseurId} — {@code ProductVariant} n'est pas {@code TenantScoped}.
     */
    @Query("""
            SELECT v FROM ProductVariant v JOIN FETCH v.product p
            WHERE p.deleted = false AND p.fournisseurId = :fid
            ORDER BY p.name, v.displayOrder
            """)
    List<ProductVariant> findAllActiveWithProductForTenant(@Param("fid") Long fournisseurId);

    @Query("""
            SELECT v FROM ProductVariant v JOIN FETCH v.product p
            WHERE v.id = :id AND p.deleted = false AND p.fournisseurId = :fid
            """)
    Optional<ProductVariant> findByIdAndFournisseurId(@Param("id") Long id, @Param("fid") Long fournisseurId);

    long countByStock(Integer stock);

    @Query("""
            SELECT COUNT(v) FROM ProductVariant v JOIN v.product p
            WHERE p.deleted = false AND p.fournisseurId = :fid AND v.stock = 0
            """)
    long countOutOfStockForTenant(@Param("fid") Long fournisseurId);

    @Query("""
            SELECT COUNT(v) FROM ProductVariant v JOIN v.product p
            WHERE p.deleted = false AND p.fournisseurId = :fid
              AND v.expiryDate IS NOT NULL AND v.expiryDate <= :limit
            """)
    long countExpiringByForTenant(@Param("fid") Long fournisseurId, @Param("limit") LocalDate limit);

    @Query("""
            SELECT COALESCE(SUM(v.price * v.stock), 0) FROM ProductVariant v JOIN v.product p
            WHERE p.deleted = false AND p.fournisseurId = :fid
            """)
    Double sumStockValueForTenant(@Param("fid") Long fournisseurId);

    @Modifying
    @Query("UPDATE ProductVariant v SET v.safetyStock = NULL WHERE v.safetyStock IS NOT NULL")
    int clearAllSafetyStockOverrides();
}
