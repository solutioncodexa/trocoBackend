package ma.codexa.troco.repository;

import ma.codexa.troco.entity.AutoPromoRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AutoPromoRuleRepository extends JpaRepository<AutoPromoRule, Long> {
    List<AutoPromoRule> findByIsActiveTrueOrderByMinOrderAmountDesc();
}
