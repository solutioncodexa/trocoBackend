package ma.codexa.troco.repository;

import ma.codexa.troco.entity.PromoCode;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PromoCodeRepository extends JpaRepository<PromoCode, Long> {
    Optional<PromoCode> findByCodeIgnoreCase(String code);
    boolean existsByCodeIgnoreCase(String code);

    long countByIsActiveTrue();

    long countByType(String type);

    @Query("SELECT p FROM PromoCode p WHERE " +
           "(:applyKeywordFilter = FALSE OR LOWER(p.code) LIKE :keywordPattern)")
    Page<PromoCode> findWithFilters(
            @Param("applyKeywordFilter") boolean applyKeywordFilter,
            @Param("keywordPattern") String keywordPattern,
            Pageable pageable);
}
