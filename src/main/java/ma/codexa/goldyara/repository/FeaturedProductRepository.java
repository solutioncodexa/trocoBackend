package ma.codexa.goldyara.repository;

import ma.codexa.goldyara.entity.FeaturedProduct;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FeaturedProductRepository extends JpaRepository<FeaturedProduct, Long> {

    List<FeaturedProduct> findBySectionOrderByDisplayOrder(FeaturedProduct.Section section);

    List<FeaturedProduct> findBySectionAndIsActiveOrderByDisplayOrder(FeaturedProduct.Section section, Boolean isActive);

    List<FeaturedProduct> findByIsActiveOrderByDisplayOrder(Boolean isActive);

    Optional<FeaturedProduct> findByProductIdAndSection(Long productId, FeaturedProduct.Section section);

    boolean existsByProductIdAndSection(Long productId, FeaturedProduct.Section section);

    @Query("SELECT MAX(fp.displayOrder) FROM FeaturedProduct fp WHERE fp.section = :section")
    Integer findMaxDisplayOrderBySection(@Param("section") FeaturedProduct.Section section);

    void deleteByProductId(Long productId);
}
