package ma.codexa.troco.repository;

import ma.codexa.troco.entity.ShippingCarrier;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ShippingCarrierRepository extends JpaRepository<ShippingCarrier, Long> {
    List<ShippingCarrier> findByFournisseurIdOrderBySortOrderAsc(Long fournisseurId);
    List<ShippingCarrier> findByFournisseurIdAndEnabledTrueOrderBySortOrderAsc(Long fournisseurId);
    Optional<ShippingCarrier> findByFournisseurIdAndCodeIgnoreCase(Long fournisseurId, String code);
}
