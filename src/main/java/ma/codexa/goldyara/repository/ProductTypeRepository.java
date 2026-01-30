package ma.codexa.goldyara.repository;

import ma.codexa.goldyara.entity.ProductType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProductTypeRepository extends JpaRepository<ProductType, Long> {

    Optional<ProductType> findByCode(String code);

    boolean existsByCode(String code);
}
