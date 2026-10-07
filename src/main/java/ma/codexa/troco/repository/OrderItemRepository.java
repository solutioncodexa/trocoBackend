package ma.codexa.troco.repository;

import ma.codexa.troco.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    List<OrderItem> findByOrderId(Long orderId);

    /** Lignes de location actives dont la période recoupe [from, to] (commandes annulées / retournées exclues). */
    @Query("SELECT i FROM OrderItem i WHERE i.product.id = :productId AND i.rentalStart IS NOT NULL "
            + "AND i.rentalStart <= :to AND i.rentalEnd >= :from AND i.order.status NOT IN ('CANCELLED', 'RETURNED')")
    List<OrderItem> findActiveRentalOverlaps(
            @Param("productId") Long productId,
            @Param("from") java.time.LocalDate from,
            @Param("to") java.time.LocalDate to);

    /** Produits souvent achetés avec productId (co-occurrence dans les commandes). */
    @Query(value = """
            SELECT oi2.product_id, COUNT(*) AS cnt
            FROM order_items oi1
            JOIN order_items oi2 ON oi1.order_id = oi2.order_id AND oi1.product_id <> oi2.product_id
            JOIN products p ON p.id = oi1.product_id
            WHERE oi1.product_id = :productId
              AND p.fournisseur_id = :fid
            GROUP BY oi2.product_id
            ORDER BY cnt DESC
            LIMIT :limit
            """, nativeQuery = true)
    List<Object[]> findFrequentlyBoughtWith(
            @Param("fid") Long fid,
            @Param("productId") Long productId,
            @Param("limit") int limit);
}
