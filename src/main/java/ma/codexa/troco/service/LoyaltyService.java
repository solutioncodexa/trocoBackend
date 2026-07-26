package ma.codexa.troco.service;

import lombok.RequiredArgsConstructor;
import ma.codexa.troco.entity.LoyaltyAccount;
import ma.codexa.troco.entity.StoreSettings;
import ma.codexa.troco.repository.LoyaltyAccountRepository;
import ma.codexa.troco.repository.StoreSettingsRepository;
import ma.codexa.troco.tenant.TenantContext;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class LoyaltyService {

    private final LoyaltyAccountRepository loyaltyAccountRepository;
    private final StoreSettingsRepository storeSettingsRepository;

    @Transactional(readOnly = true)
    public Map<String, Object> balance(String phone) {
        Long fid = TenantContext.requireFournisseurId();
        StoreSettings settings = settings(fid);
        Map<String, Object> out = new LinkedHashMap<>();
        boolean enabled = Boolean.TRUE.equals(settings.getLoyaltyEnabled());
        out.put("enabled", enabled);
        out.put("pointsPerMad", settings.getLoyaltyPointsPerMad());
        out.put("madPerPoint", settings.getLoyaltyMadPerPoint());
        if (!enabled || phone == null || phone.isBlank()) {
            out.put("points", 0);
            return out;
        }
        int points = loyaltyAccountRepository.findByFournisseurIdAndPhone(fid, normalizePhone(phone))
                .map(LoyaltyAccount::getPointsBalance)
                .orElse(0);
        out.put("points", points);
        return out;
    }

    @Transactional
    public int earn(String phone, String email, double orderTotalMad) {
        Long fid = TenantContext.requireFournisseurId();
        StoreSettings settings = settings(fid);
        if (!Boolean.TRUE.equals(settings.getLoyaltyEnabled()) || phone == null || phone.isBlank() || orderTotalMad <= 0) {
            return 0;
        }
        BigDecimal rate = settings.getLoyaltyPointsPerMad() != null
                ? settings.getLoyaltyPointsPerMad() : BigDecimal.ONE;
        int earned = BigDecimal.valueOf(orderTotalMad).multiply(rate).setScale(0, RoundingMode.DOWN).intValue();
        if (earned <= 0) return 0;
        LoyaltyAccount acc = getOrCreate(fid, phone, email);
        acc.setPointsBalance(acc.getPointsBalance() + earned);
        loyaltyAccountRepository.save(acc);
        return earned;
    }

    @Transactional
    public record RedeemResult(int pointsRedeemed, double discountMad) {}

    @Transactional
    public RedeemResult redeem(String phone, Integer requestedPoints, double orderTotalMad) {
        Long fid = TenantContext.requireFournisseurId();
        StoreSettings settings = settings(fid);
        if (!Boolean.TRUE.equals(settings.getLoyaltyEnabled()) || requestedPoints == null || requestedPoints <= 0
                || phone == null || phone.isBlank()) {
            return new RedeemResult(0, 0);
        }
        LoyaltyAccount acc = getOrCreate(fid, phone, null);
        int use = Math.min(requestedPoints, acc.getPointsBalance());
        BigDecimal madPerPoint = settings.getLoyaltyMadPerPoint() != null
                ? settings.getLoyaltyMadPerPoint() : new BigDecimal("0.10");
        double discount = BigDecimal.valueOf(use).multiply(madPerPoint).doubleValue();
        discount = Math.min(discount, Math.max(0, orderTotalMad - 1));
        use = madPerPoint.doubleValue() > 0
                ? BigDecimal.valueOf(discount).divide(madPerPoint, 0, RoundingMode.DOWN).intValue()
                : 0;
        if (use <= 0) return new RedeemResult(0, 0);
        acc.setPointsBalance(acc.getPointsBalance() - use);
        loyaltyAccountRepository.save(acc);
        return new RedeemResult(use, discount);
    }

    private LoyaltyAccount getOrCreate(Long fid, String phone, String email) {
        String p = normalizePhone(phone);
        return loyaltyAccountRepository.findByFournisseurIdAndPhone(fid, p).orElseGet(() -> {
            LoyaltyAccount a = new LoyaltyAccount();
            a.setFournisseurId(fid);
            a.setPhone(p);
            a.setEmail(email);
            a.setPointsBalance(0);
            return loyaltyAccountRepository.save(a);
        });
    }

    private StoreSettings settings(Long fid) {
        return storeSettingsRepository.findByFournisseurId(fid)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Settings introuvables"));
    }

    private String normalizePhone(String phone) {
        return phone.replaceAll("[^0-9+]", "").toLowerCase(Locale.ROOT);
    }
}
