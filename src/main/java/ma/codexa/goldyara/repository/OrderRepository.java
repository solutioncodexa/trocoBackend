package ma.codexa.goldyara.repository;

import ma.codexa.goldyara.entity.Order;
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
public interface OrderRepository extends JpaRepository<Order, Long> {

    /**
     * Un seul "bag" (orderItems) par requête : ne pas grapher product.images sous peine de
     * MultipleBagFetchException (orderItems + images sont tous deux {@code List} sans {@code @OrderColumn}).
     * Les images sont chargées en sous-requête grâce à {@code @Fetch(SUBSELECT)} sur {@link Product#images}.
     */
    @EntityGraph(attributePaths = {"customer", "orderItems", "orderItems.product"})
    @Query("SELECT o FROM Order o ORDER BY o.createdAt DESC")
    List<Order> findAllWithDetails();

    @EntityGraph(attributePaths = {"customer", "orderItems", "orderItems.product"})
    @Query(value = "SELECT o FROM Order o",
           countQuery = "SELECT COUNT(o) FROM Order o")
    Page<Order> findAllWithDetails(Pageable pageable);

    @EntityGraph(attributePaths = {"customer", "orderItems", "orderItems.product"})
    @Query(value = "SELECT o FROM Order o WHERE " +
           "(:status IS NULL OR o.status = :status) AND " +
           "(:applyKeywordFilter = FALSE OR LOWER(o.customer.fullName) LIKE :keywordPattern OR o.customer.phone LIKE :keywordPattern OR LOWER(o.orderNumber) LIKE :keywordPattern)",
           countQuery = "SELECT COUNT(o) FROM Order o WHERE " +
           "(:status IS NULL OR o.status = :status) AND " +
           "(:applyKeywordFilter = FALSE OR LOWER(o.customer.fullName) LIKE :keywordPattern OR o.customer.phone LIKE :keywordPattern OR LOWER(o.orderNumber) LIKE :keywordPattern)")
    Page<Order> findWithFilters(
            @Param("status") String status,
            @Param("applyKeywordFilter") boolean applyKeywordFilter,
            @Param("keywordPattern") String keywordPattern,
            Pageable pageable);

    /** Idem {@link #findAllWithDetails()} — une seule collection bag graphee. */
    @EntityGraph(attributePaths = {"customer", "orderItems", "orderItems.product"})
    @Query("SELECT o FROM Order o WHERE o.id = :id")
    Optional<Order> findByIdWithDetails(@Param("id") Long id);

    @EntityGraph(attributePaths = {"customer", "orderItems", "orderItems.product"})
    Optional<Order> findByOrderNumber(String orderNumber);

    @EntityGraph(attributePaths = {"customer", "orderItems", "orderItems.product"})
    List<Order> findByStatus(String status);

    @EntityGraph(attributePaths = {"customer", "orderItems"})
    List<Order> findByCustomerId(Long customerId);

    long countByStatus(String status);
}
