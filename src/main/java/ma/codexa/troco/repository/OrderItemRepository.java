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
