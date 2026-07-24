package ma.codexa.troco.repository;

import ma.codexa.troco.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
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

    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o WHERE o.status = :status AND o.createdAt >= :from AND o.createdAt < :to")
    Double sumTotalByStatusAndCreatedBetween(
            @Param("status") String status,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);

    @Query("SELECT COUNT(o) FROM Order o WHERE o.status = :status AND o.createdAt >= :from AND o.createdAt < :to")
    long countByStatusAndCreatedBetween(
            @Param("status") String status,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);

    @Query("SELECT COUNT(o) FROM Order o WHERE o.createdAt >= :from AND o.createdAt < :to")
    long countByCreatedBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("SELECT FUNCTION('DATE', o.createdAt), COALESCE(SUM(o.totalAmount), 0), COUNT(o) " +
           "FROM Order o WHERE o.status = 'DELIVERED' AND o.createdAt >= :from AND o.createdAt < :to " +
           "GROUP BY FUNCTION('DATE', o.createdAt) ORDER BY FUNCTION('DATE', o.createdAt)")
    List<Object[]> dailyDeliveredRevenue(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("SELECT p.id, p.name, SUM(oi.quantity), COALESCE(SUM(oi.subtotal), 0) " +
           "FROM OrderItem oi JOIN oi.order o JOIN oi.product p " +
           "WHERE o.status = 'DELIVERED' AND o.createdAt >= :from AND o.createdAt < :to " +
           "GROUP BY p.id, p.name ORDER BY SUM(oi.subtotal) DESC")
    List<Object[]> topProductsByRevenue(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);
}
