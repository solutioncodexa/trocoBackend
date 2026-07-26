package ma.codexa.troco.repository;

import ma.codexa.troco.entity.ProductReview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProductReviewRepository extends JpaRepository<ProductReview, Long> {

    List<ProductReview> findByProductIdAndApprovedTrueOrderByCreatedAtDesc(Long productId);

    List<ProductReview> findAllByOrderByCreatedAtDesc();

    @Query("""
            SELECT COALESCE(AVG(r.rating), 0), COUNT(r)
            FROM ProductReview r
            WHERE r.productId = :productId AND r.approved = true
            """)
    Object[] avgAndCount(@Param("productId") Long productId);
}
