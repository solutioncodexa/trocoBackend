package ma.codexa.troco.repository;

import ma.codexa.troco.entity.Plan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PlanRepository extends JpaRepository<Plan, Long> {
    Optional<Plan> findByCodeIgnoreCase(String code);
    List<Plan> findByActiveTrueOrderByPriceMadAsc();
}
