package ma.codexa.troco.service;

import lombok.RequiredArgsConstructor;
import ma.codexa.troco.dto.ShippingCarrierDTO;
import ma.codexa.troco.entity.ShippingCarrier;
import ma.codexa.troco.entity.ShippingCityRate;
import ma.codexa.troco.market.CityKey;
import ma.codexa.troco.repository.ShippingCarrierRepository;
import ma.codexa.troco.repository.ShippingCityRateRepository;
import ma.codexa.troco.tenant.TenantContext;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class ShippingCarrierService {

    private final ShippingCarrierRepository shippingCarrierRepository;
    private final ShippingCityRateRepository shippingCityRateRepository;

    @Transactional(readOnly = true)
    public List<ShippingCarrierDTO> listPublic(BigDecimal cartSubtotal) {
        return listPublic(cartSubtotal, null);
    }

    @Transactional(readOnly = true)
    public List<ShippingCarrierDTO> listPublic(BigDecimal cartSubtotal, String city) {
        Long fid = TenantContext.requireFournisseurId();
        return shippingCarrierRepository.findByFournisseurIdAndEnabledTrueOrderBySortOrderAsc(fid).stream()
                .map(c -> toDto(c, quoteFee(c, cartSubtotal, city)))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ShippingCarrierDTO> listAdmin() {
        Long fid = TenantContext.requireFournisseurId();
        return shippingCarrierRepository.findByFournisseurIdOrderBySortOrderAsc(fid).stream()
                .map(c -> toDto(c, null))
                .toList();
    }

    @Transactional
    public ShippingCarrierDTO upsert(ShippingCarrierDTO req) {
        Long fid = TenantContext.requireFournisseurId();
        if (req.code() == null || req.code().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "code transporteur requis");
        }
        String code = req.code().trim().toUpperCase(Locale.ROOT);
        ShippingCarrier c = shippingCarrierRepository.findByFournisseurIdAndCodeIgnoreCase(fid, code)
                .orElseGet(() -> {
                    ShippingCarrier n = new ShippingCarrier();
                    n.setFournisseurId(fid);
                    n.setCode(code);
                    return n;
                });
        if (req.name() != null) c.setName(req.name());
        c.setEnabled(req.enabled());
        if (req.baseFee() != null) c.setBaseFee(req.baseFee());
        c.setFreeAbove(req.freeAbove());
        if (req.trackingUrlTemplate() != null) c.setTrackingUrlTemplate(req.trackingUrlTemplate());
        if (req.etaDaysMin() != null) c.setEtaDaysMin(req.etaDaysMin());
        if (req.etaDaysMax() != null) c.setEtaDaysMax(req.etaDaysMax());
        if (req.sortOrder() != null) c.setSortOrder(req.sortOrder());
        return toDto(shippingCarrierRepository.save(c), null);
    }

    @Transactional(readOnly = true)
    public BigDecimal quote(String carrierCode, BigDecimal cartSubtotal) {
        return quote(carrierCode, cartSubtotal, null);
    }

    @Transactional(readOnly = true)
    public BigDecimal quote(String carrierCode, BigDecimal cartSubtotal, String city) {
        Long fid = TenantContext.requireFournisseurId();
        ShippingCarrier c = shippingCarrierRepository.findByFournisseurIdAndCodeIgnoreCase(fid, carrierCode)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Transporteur introuvable"));
        if (!c.isEnabled()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Transporteur désactivé");
        }
        return quoteFee(c, cartSubtotal, city);
    }

    public String buildTrackingUrl(ShippingCarrier carrier, String trackingNumber) {
        if (carrier == null || trackingNumber == null || trackingNumber.isBlank()) return null;
        String tpl = carrier.getTrackingUrlTemplate();
        if (tpl == null || tpl.isBlank()) return null;
        return tpl.replace("{tracking}", trackingNumber.trim());
    }

    @Transactional(readOnly = true)
    public ShippingCarrier requireEnabled(String carrierCode) {
        Long fid = TenantContext.requireFournisseurId();
        return shippingCarrierRepository.findByFournisseurIdAndCodeIgnoreCase(fid, carrierCode)
                .filter(ShippingCarrier::isEnabled)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Transporteur invalide"));
    }

    private BigDecimal quoteFee(ShippingCarrier c, BigDecimal cartSubtotal, String city) {
        BigDecimal sub = cartSubtotal != null ? cartSubtotal : BigDecimal.ZERO;
        if (c.getFreeAbove() != null && sub.compareTo(c.getFreeAbove()) >= 0) {
            return BigDecimal.ZERO;
        }
        String key = CityKey.of(city);
        if (!key.isBlank() && c.getFournisseurId() != null) {
            return shippingCityRateRepository
                    .findByFournisseurIdAndCarrierCodeIgnoreCaseAndCityKey(c.getFournisseurId(), c.getCode(), key)
                    .map(ShippingCityRate::getFee)
                    .orElse(c.getBaseFee() != null ? c.getBaseFee() : BigDecimal.ZERO);
        }
        return c.getBaseFee() != null ? c.getBaseFee() : BigDecimal.ZERO;
    }

    private ShippingCarrierDTO toDto(ShippingCarrier c, BigDecimal quoted) {
        return new ShippingCarrierDTO(
                c.getId(), c.getCode(), c.getName(), c.isEnabled(),
                c.getBaseFee(), c.getFreeAbove(), c.getTrackingUrlTemplate(),
                c.getEtaDaysMin(), c.getEtaDaysMax(), c.getSortOrder(), quoted
        );
    }
}
