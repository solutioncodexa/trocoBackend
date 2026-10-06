package ma.codexa.troco.repository;

import ma.codexa.troco.entity.ReferralCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ReferralCodeRepository extends JpaRepository<ReferralCode, Long> {
    List<ReferralCode> findByFournisseurIdOrderByCreatedAtDesc(Long fournisseurId);

    Optional<ReferralCode> findByFournisseurIdAndCodeIgnoreCase(Long fournisseurId, String code);
}
