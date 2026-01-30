package ma.codexa.goldyara.repository;

import ma.codexa.goldyara.entity.GoldType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GoldTypeRepository extends JpaRepository<GoldType, Long> {

    List<GoldType> findAllByOrderBySortOrderAsc();

    Optional<GoldType> findByCodeIgnoreCase(String code);
}
