package ma.codexa.goldyara.repository;

import ma.codexa.goldyara.entity.ProductVariant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductVariantRepository extends JpaRepository<ProductVariant, Long> {

    List<ProductVariant> findByProductIdOrderByDisplayOrderAsc(Long productId);

    void deleteByProductId(Long productId);
}
