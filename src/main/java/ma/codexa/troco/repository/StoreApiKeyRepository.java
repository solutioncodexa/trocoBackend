package ma.codexa.troco.repository;

import ma.codexa.troco.entity.StoreApiKey;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StoreApiKeyRepository extends JpaRepository<StoreApiKey, Long> {
    List<StoreApiKey> findByFournisseurIdOrderByCreatedAtDesc(Long fournisseurId);
    Optional<StoreApiKey> findByKeyPrefixAndEnabledTrueAndRevokedAtIsNull(String keyPrefix);
}
