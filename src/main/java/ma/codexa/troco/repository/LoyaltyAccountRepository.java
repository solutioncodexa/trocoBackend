package ma.codexa.troco.repository;

import ma.codexa.troco.entity.LoyaltyAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LoyaltyAccountRepository extends JpaRepository<LoyaltyAccount, Long> {
    Optional<LoyaltyAccount> findByFournisseurIdAndPhone(Long fournisseurId, String phone);
}
