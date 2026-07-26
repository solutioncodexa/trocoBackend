package ma.codexa.troco.repository;

import ma.codexa.troco.entity.SubscriptionPayment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SubscriptionPaymentRepository extends JpaRepository<SubscriptionPayment, Long> {
    Optional<SubscriptionPayment> findByOid(String oid);

    List<SubscriptionPayment> findByFournisseurIdOrderByCreatedAtDesc(Long fournisseurId);
}
