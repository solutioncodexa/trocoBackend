package ma.codexa.goldyara.repository;

import ma.codexa.goldyara.entity.CustomOrder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CustomOrderRepository extends JpaRepository<CustomOrder, Long> {

    @EntityGraph(attributePaths = {"customer", "referenceImages"})
    @Query("SELECT c FROM CustomOrder c ORDER BY c.createdAt DESC")
    List<CustomOrder> findAllWithDetails();

    @EntityGraph(attributePaths = {"customer", "referenceImages"})
    @Query(value = "SELECT c FROM CustomOrder c",
           countQuery = "SELECT COUNT(c) FROM CustomOrder c")
    Page<CustomOrder> findAllWithDetails(Pageable pageable);

    @EntityGraph(attributePaths = {"customer", "referenceImages"})
    @Query(value = "SELECT c FROM CustomOrder c WHERE " +
           "(:status IS NULL OR c.status = :status) AND " +
           "(:applyKeywordFilter = FALSE OR LOWER(c.customer.fullName) LIKE :keywordPattern OR LOWER(COALESCE(c.customer.email, '')) LIKE :keywordPattern)",
           countQuery = "SELECT COUNT(c) FROM CustomOrder c WHERE " +
           "(:status IS NULL OR c.status = :status) AND " +
           "(:applyKeywordFilter = FALSE OR LOWER(c.customer.fullName) LIKE :keywordPattern OR LOWER(COALESCE(c.customer.email, '')) LIKE :keywordPattern)")
    Page<CustomOrder> findWithFilters(
            @Param("status") String status,
            @Param("applyKeywordFilter") boolean applyKeywordFilter,
            @Param("keywordPattern") String keywordPattern,
            Pageable pageable);

    @EntityGraph(attributePaths = {"customer", "referenceImages"})
    @Query("SELECT c FROM CustomOrder c WHERE c.id = :id")
    Optional<CustomOrder> findDetailedById(@Param("id") Long id);

    @EntityGraph(attributePaths = {"customer", "referenceImages"})
    @Query("SELECT c FROM CustomOrder c WHERE c.status = :status ORDER BY c.createdAt DESC")
    List<CustomOrder> findByStatusWithDetails(@Param("status") String status);

    List<CustomOrder> findByStatus(String status);

    List<CustomOrder> findByCustomerId(Long customerId);

    long countByStatus(String status);
}
