package ma.codexa.troco.repository;

import ma.codexa.troco.entity.HomeHeroSettings;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface HomeHeroSettingsRepository extends JpaRepository<HomeHeroSettings, Long> {
    Optional<HomeHeroSettings> findFirstByFournisseurId(Long fournisseurId);
}
