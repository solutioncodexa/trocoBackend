package ma.codexa.troco.repository;

import ma.codexa.troco.entity.ProductReview;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProductReviewRepository extends JpaRepository<ProductReview, Long> {

    List<ProductReview> findByProductIdAndApprovedTrueOrderByCreatedAtDesc(Long productId);

    Page<ProductReview> findByProductIdAndApprovedTrueOrderByCreatedAtDesc(Long productId, Pageable pageable);

    List<ProductReview> findAllByOrderByCreatedAtDesc();

    long countByProductIdAndApprovedTrue(Long productId);

    @Query("""
            SELECT COALESCE(AVG(r.rating), 0)
            FROM ProductReview r
            WHERE r.productId = :productId AND r.approved = true
            """)
    Double avgRating(@Param("productId") Long productId);
}
