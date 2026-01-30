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

    /** Fetch all orders with customer and items eagerly loaded (prevents N+1). */
    @EntityGraph(attributePaths = {"customer", "orderItems", "orderItems.product"})
    @Query("SELECT o FROM Order o ORDER BY o.createdAt DESC")
    List<Order> findAllWithDetails();

    /** Fetch single order with all relations. */
    @EntityGraph(attributePaths = {"customer", "orderItems", "orderItems.product", "orderItems.product.images"})
    @Query("SELECT o FROM Order o WHERE o.id = :id")
    Optional<Order> findByIdWithDetails(@Param("id") Long id);

    Optional<Order> findByOrderNumber(String orderNumber);

    @EntityGraph(attributePaths = {"customer", "orderItems", "orderItems.product"})
    List<Order> findByStatus(String status);

    @EntityGraph(attributePaths = {"customer", "orderItems"})
    List<Order> findByCustomerId(Long customerId);
}
