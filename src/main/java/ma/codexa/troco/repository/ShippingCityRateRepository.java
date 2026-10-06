package ma.codexa.troco.repository;

import ma.codexa.troco.entity.ShippingCityRate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ShippingCityRateRepository extends JpaRepository<ShippingCityRate, Long> {
    List<ShippingCityRate> findByFournisseurIdOrderByCarrierCodeAscCityAsc(Long fournisseurId);

    Optional<ShippingCityRate> findByFournisseurIdAndCarrierCodeIgnoreCaseAndCityKey(
            Long fournisseurId, String carrierCode, String cityKey);
}
