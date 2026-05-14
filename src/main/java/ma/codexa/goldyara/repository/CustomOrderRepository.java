package ma.codexa.goldyara.repository;

import ma.codexa.goldyara.entity.CustomOrder;
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
    @Query("SELECT c FROM CustomOrder c WHERE c.id = :id")
    Optional<CustomOrder> findDetailedById(@Param("id") Long id);

    @EntityGraph(attributePaths = {"customer", "referenceImages"})
    @Query("SELECT c FROM CustomOrder c WHERE c.status = :status ORDER BY c.createdAt DESC")
    List<CustomOrder> findByStatusWithDetails(@Param("status") String status);

    List<CustomOrder> findByStatus(String status);

    List<CustomOrder> findByCustomerId(Long customerId);
}
