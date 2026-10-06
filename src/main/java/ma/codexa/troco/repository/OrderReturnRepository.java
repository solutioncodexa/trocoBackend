package ma.codexa.troco.repository;

import ma.codexa.troco.entity.OrderReturn;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderReturnRepository extends JpaRepository<OrderReturn, Long> {
    List<OrderReturn> findByFournisseurIdOrderByCreatedAtDesc(Long fournisseurId);
}
