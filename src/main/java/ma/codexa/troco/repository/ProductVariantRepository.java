package ma.codexa.troco.repository;

import ma.codexa.troco.entity.ProductVariant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ProductVariantRepository extends JpaRepository<ProductVariant, Long> {

    List<ProductVariant> findByProductIdOrderByDisplayOrderAsc(Long productId);

    void deleteByProductId(Long productId);

    @Query("SELECT v FROM ProductVariant v JOIN FETCH v.product p WHERE p.deleted = false ORDER BY p.name, v.displayOrder")
    List<ProductVariant> findAllActiveWithProduct();

    long countByStock(Integer stock);

    @Query("SELECT COUNT(v) FROM ProductVariant v JOIN v.product p WHERE p.deleted = false AND v.stock = 0")
    long countOutOfStock();

    @Query("SELECT COUNT(v) FROM ProductVariant v JOIN v.product p WHERE p.deleted = false AND v.expiryDate IS NOT NULL AND v.expiryDate <= :limit")
    long countExpiringBy(@Param("limit") LocalDate limit);

    @Query("SELECT COALESCE(SUM(v.price * v.stock), 0) FROM ProductVariant v JOIN v.product p WHERE p.deleted = false")
    Double sumStockValue();

    @Modifying
    @Query("UPDATE ProductVariant v SET v.safetyStock = NULL WHERE v.safetyStock IS NOT NULL")
    int clearAllSafetyStockOverrides();
}
