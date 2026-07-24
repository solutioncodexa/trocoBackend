package ma.codexa.troco.repository;

import ma.codexa.troco.entity.StockMovement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StockMovementRepository extends JpaRepository<StockMovement, Long> {

    Page<StockMovement> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Page<StockMovement> findByVariantIdOrderByCreatedAtDesc(Long variantId, Pageable pageable);

    Page<StockMovement> findByTypeOrderByCreatedAtDesc(String type, Pageable pageable);

    Page<StockMovement> findByVariantIdAndTypeOrderByCreatedAtDesc(Long variantId, String type, Pageable pageable);

    boolean existsByOrderIdAndType(Long orderId, String type);
}
