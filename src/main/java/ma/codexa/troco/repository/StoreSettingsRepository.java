package ma.codexa.troco.repository;

import ma.codexa.troco.entity.StoreSettings;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StoreSettingsRepository extends JpaRepository<StoreSettings, Long> {
    Optional<StoreSettings> findByFournisseurId(Long fournisseurId);
}
