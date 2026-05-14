package ma.codexa.goldyara.repository;

import ma.codexa.goldyara.entity.Order;
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
}
