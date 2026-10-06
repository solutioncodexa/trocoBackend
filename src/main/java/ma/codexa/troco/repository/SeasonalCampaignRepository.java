package ma.codexa.troco.repository;

import ma.codexa.troco.entity.SeasonalCampaign;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SeasonalCampaignRepository extends JpaRepository<SeasonalCampaign, Long> {
    List<SeasonalCampaign> findByFournisseurIdOrderByCodeAsc(Long fournisseurId);

    Optional<SeasonalCampaign> findByFournisseurIdAndCodeIgnoreCase(Long fournisseurId, String code);
}
