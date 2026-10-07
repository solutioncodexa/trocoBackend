package ma.codexa.troco.market;

import lombok.RequiredArgsConstructor;
import ma.codexa.troco.common.exception.BusinessException;
import ma.codexa.troco.entity.*;
import ma.codexa.troco.repository.*;
import ma.codexa.troco.service.StockService;
import ma.codexa.troco.tenant.TenantContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class MarketService {

    private final StoreSettingsRepository storeSettingsRepository;
    private final ShippingCityRateRepository shippingCityRateRepository;
    private final SeasonalCampaignRepository seasonalCampaignRepository;
    private final ReferralCodeRepository referralCodeRepository;
    private final OrderReturnRepository orderReturnRepository;
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final FournisseurRepository fournisseurRepository;
    private final StockService stockService;

    @Value("${app.public.site-url:http://localhost:4200}")
    private String publicSiteUrl;

    @Transactional(readOnly = true)
    public Map<String, Object> publicConfig() {
        StoreSettings s = currentSettings();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("payzoneEnabled", s != null && Boolean.TRUE.equals(s.getPaymentPayzoneEnabled()));
        body.put("transferEnabled", s != null && Boolean.TRUE.equals(s.getPaymentTransferEnabled()));
        body.put("transferInstructions", s != null ? s.getTransferInstructions() : null);
        body.put("campaign", activeCampaign());
        body.put("metaCatalogPath", "/catalog/meta.csv");
        return body;
    }

    @Transactional
    public Map<String, Object> updateConfig(Map<String, Object> body) {
        StoreSettings s = requireSettings();
        if (body.containsKey("payzoneEnabled")) {
            s.setPaymentPayzoneEnabled(Boolean.TRUE.equals(body.get("payzoneEnabled")));
        }
        if (body.containsKey("transferEnabled")) {
            s.setPaymentTransferEnabled(Boolean.TRUE.equals(body.get("transferEnabled")));
        }
        if (body.containsKey("transferInstructions")) {
            Object raw = body.get("transferInstructions");
            s.setTransferInstructions(raw == null || String.valueOf(raw).isBlank() ? null : String.valueOf(raw).trim());
        }
        storeSettingsRepository.save(s);
        return publicConfig();
    }

    public static final int EMAIL_NOTE_MAX = 1000;
    public static final int EMAIL_SIGNATURE_MAX = 500;

    @Transactional(readOnly = true)
    public Map<String, Object> emailTemplates() {
        StoreSettings s = requireSettings();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("note", s.getCustomerEmailNote());
        body.put("signature", s.getCustomerEmailSignature());
        return body;
    }

    @Transactional
    public Map<String, Object> updateEmailTemplates(Map<String, Object> body) {
        StoreSettings s = requireSettings();
        if (body.containsKey("note")) {
            s.setCustomerEmailNote(cleanText(body.get("note"), EMAIL_NOTE_MAX, "Le message"));
        }
        if (body.containsKey("signature")) {
            s.setCustomerEmailSignature(cleanText(body.get("signature"), EMAIL_SIGNATURE_MAX, "La signature"));
        }
        storeSettingsRepository.save(s);
        return emailTemplates();
    }

    private static String cleanText(Object raw, int max, String label) {
        if (raw == null) return null;
        String v = String.valueOf(raw).replace("\r\n", "\n").trim();
        if (v.isEmpty()) return null;
        if (v.length() > max) {
            throw new BusinessException(label + " est trop long (" + max + " caractères maximum)", HttpStatus.BAD_REQUEST);
        }
        return v;
    }

    @Transactional(readOnly = true)
    public List<ShippingCityRate> listCityRates() {
        return shippingCityRateRepository.findByFournisseurIdOrderByCarrierCodeAscCityAsc(requireFid());
    }

    @Transactional
    public ShippingCityRate upsertCityRate(String carrierCode, String city, BigDecimal fee) {
        if (carrierCode == null || carrierCode.isBlank() || city == null || city.isBlank()) {
            throw new BusinessException("Transporteur et ville requis", HttpStatus.BAD_REQUEST);
        }
        if (fee == null || fee.signum() < 0) {
            throw new BusinessException("Frais invalides", HttpStatus.BAD_REQUEST);
        }
        Long fid = requireFid();
        String code = carrierCode.trim().toUpperCase(Locale.ROOT);
        String key = CityKey.of(city);
        ShippingCityRate row = shippingCityRateRepository
                .findByFournisseurIdAndCarrierCodeIgnoreCaseAndCityKey(fid, code, key)
                .orElseGet(() -> {
                    ShippingCityRate n = new ShippingCityRate();
                    n.setFournisseurId(fid);
                    n.setCarrierCode(code);
                    n.setCityKey(key);
                    return n;
                });
        row.setCity(city.trim());
        row.setFee(fee.setScale(2, RoundingMode.HALF_UP));
        return shippingCityRateRepository.save(row);
    }

    @Transactional
    public void deleteCityRate(Long id) {
        ShippingCityRate row = shippingCityRateRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Barème introuvable", HttpStatus.NOT_FOUND));
        shippingCityRateRepository.delete(row);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> activeCampaign() {
        Long fid = TenantContext.getFournisseurId();
        if (fid == null) return null;
        LocalDate today = LocalDate.now();
        return seasonalCampaignRepository.findByFournisseurIdOrderByCodeAsc(fid).stream()
                .filter(SeasonalCampaign::isEnabled)
                .filter(c -> c.getStartsOn() == null || !today.isBefore(c.getStartsOn()))
                .filter(c -> c.getEndsOn() == null || !today.isAfter(c.getEndsOn()))
                .findFirst()
                .map(this::campaignMap)
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public List<SeasonalCampaign> listCampaigns() {
        return seasonalCampaignRepository.findByFournisseurIdOrderByCodeAsc(requireFid());
    }

    @Transactional
    public SeasonalCampaign saveCampaign(SeasonalCampaign incoming) {
        if (incoming.getCode() == null || incoming.getCode().isBlank()) {
            throw new BusinessException("Code campagne requis", HttpStatus.BAD_REQUEST);
        }
        Long fid = requireFid();
        String code = incoming.getCode().trim().toUpperCase(Locale.ROOT);
        SeasonalCampaign row = seasonalCampaignRepository.findByFournisseurIdAndCodeIgnoreCase(fid, code)
                .orElseGet(() -> {
                    SeasonalCampaign n = new SeasonalCampaign();
                    n.setFournisseurId(fid);
                    n.setCode(code);
                    return n;
                });
        if (incoming.getTitle() != null && !incoming.getTitle().isBlank()) row.setTitle(incoming.getTitle().trim());
        else if (row.getTitle() == null) row.setTitle(code);
        if (incoming.getDiscountPercent() != null) row.setDiscountPercent(incoming.getDiscountPercent());
        row.setStartsOn(incoming.getStartsOn());
        row.setEndsOn(incoming.getEndsOn());
        row.setEnabled(incoming.isEnabled());
        return seasonalCampaignRepository.save(row);
    }

    /** Réduction MAD d'une campagne active, sans incrémenter quoi que ce soit. */
    @Transactional(readOnly = true)
    public double seasonalDiscount(String code, double merchandiseTotal) {
        if (code == null || code.isBlank() || merchandiseTotal <= 0) return 0;
        SeasonalCampaign c = seasonalCampaignRepository
                .findByFournisseurIdAndCodeIgnoreCase(requireFid(), code.trim())
                .filter(SeasonalCampaign::isEnabled)
                .orElse(null);
        if (c == null || c.getDiscountPercent() == null) return 0;
        LocalDate today = LocalDate.now();
        if (c.getStartsOn() != null && today.isBefore(c.getStartsOn())) return 0;
        if (c.getEndsOn() != null && today.isAfter(c.getEndsOn())) return 0;
        double pct = c.getDiscountPercent().doubleValue();
        if (pct <= 0) return 0;
        return Math.min(merchandiseTotal, merchandiseTotal * (pct / 100.0));
    }

    @Transactional(readOnly = true)
    public List<ReferralCode> listReferrals() {
        return referralCodeRepository.findByFournisseurIdOrderByCreatedAtDesc(requireFid());
    }

    @Transactional
    public ReferralCode saveReferral(String code, BigDecimal reward, String label) {
        if (code == null || code.isBlank()) {
            throw new BusinessException("Code parrain requis", HttpStatus.BAD_REQUEST);
        }
        Long fid = requireFid();
        String normalized = code.trim().toUpperCase(Locale.ROOT);
        ReferralCode row = referralCodeRepository.findByFournisseurIdAndCodeIgnoreCase(fid, normalized)
                .orElseGet(() -> {
                    ReferralCode n = new ReferralCode();
                    n.setFournisseurId(fid);
                    n.setCode(normalized);
                    return n;
                });
        row.setRewardMad(reward != null && reward.signum() >= 0 ? reward : BigDecimal.ZERO);
        row.setReferrerLabel(label == null || label.isBlank() ? null : label.trim());
        row.setActive(true);
        return referralCodeRepository.save(row);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> previewReferral(String code) {
        ReferralCode row = findActiveReferral(code);
        return Map.of(
                "code", row.getCode(),
                "rewardMad", row.getRewardMad(),
                "referrerLabel", row.getReferrerLabel() == null ? "" : row.getReferrerLabel()
        );
    }

    /** Consomme un code et retourne la réduction MAD plafonnée au restant. */
    @Transactional
    public double consumeReferral(String code, double merchandiseTotal) {
        if (code == null || code.isBlank() || merchandiseTotal <= 0) return 0;
        ReferralCode row = findActiveReferral(code);
        double reward = row.getRewardMad() != null ? row.getRewardMad().doubleValue() : 0;
        double applied = Math.min(merchandiseTotal, Math.max(0, reward));
        row.setUsesCount(row.getUsesCount() + 1);
        referralCodeRepository.save(row);
        return applied;
    }

    @Transactional
    public OrderReturn requestReturn(String orderNumber, String phone, String reason) {
        if (orderNumber == null || orderNumber.isBlank() || phone == null || phone.isBlank()) {
            throw new BusinessException("Numéro de commande et téléphone requis", HttpStatus.BAD_REQUEST);
        }
        Order order = orderRepository.findByOrderNumber(orderNumber.trim())
                .orElseThrow(() -> new BusinessException("Commande introuvable", HttpStatus.NOT_FOUND));
        String stored = order.getCustomer() != null ? digits(order.getCustomer().getPhone()) : "";
        String given = digits(phone);
        if (stored.length() < 8 || given.length() < 8 || !stored.endsWith(given.substring(given.length() - 8))
                && !given.endsWith(stored.substring(stored.length() - 8))) {
            throw new BusinessException("Le téléphone ne correspond pas à la commande", HttpStatus.BAD_REQUEST);
        }
        OrderReturn row = new OrderReturn();
        row.setFournisseurId(order.getFournisseurId());
        row.setOrderId(order.getId());
        row.setReason(reason == null ? null : reason.trim());
        row.setCustomerPhone(phone.trim());
        row.setStatus("REQUESTED");
        row.setRefundAmount(BigDecimal.valueOf(order.getTotalAmount() != null ? order.getTotalAmount() : 0));
        return orderReturnRepository.save(row);
    }

    @Transactional(readOnly = true)
    public List<OrderReturn> listReturns() {
        return orderReturnRepository.findByFournisseurIdOrderByCreatedAtDesc(requireFid());
    }

    @Transactional
    public OrderReturn updateReturn(Long id, String status) {
        OrderReturn row = orderReturnRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Retour introuvable", HttpStatus.NOT_FOUND));
        String next = status == null ? "" : status.trim().toUpperCase(Locale.ROOT);
        if (!List.of("REQUESTED", "APPROVED", "REFUNDED", "REJECTED").contains(next)) {
            throw new BusinessException("Statut de retour inconnu", HttpStatus.BAD_REQUEST);
        }
        String previous = row.getStatus();
        row.setStatus(next);
        if ("REFUNDED".equals(next) && !"REFUNDED".equals(previous)) {
            Order order = orderRepository.findByIdWithDetails(row.getOrderId())
                    .orElseThrow(() -> new BusinessException("Commande introuvable", HttpStatus.NOT_FOUND));
            String before = order.getStatus();
            order.setPaymentStatus("refunded");
            order.setStatus("RETURNED");
            orderRepository.save(order);
            if (!OrderStatuses.restoresStock(before)) {
                stockService.restoreForOrder(order);
            }
            if (row.getRefundAmount() == null) {
                row.setRefundAmount(BigDecimal.valueOf(order.getTotalAmount() != null ? order.getTotalAmount() : 0));
            }
        }
        return orderReturnRepository.save(row);
    }

    @Transactional(readOnly = true)
    public String ordersCsv() {
        StringBuilder csv = new StringBuilder();
        csv.append("orderNumber,createdAt,customer,phone,city,status,paymentMethod,total\n");
        for (Order order : orderRepository.findAllWithDetails()) {
            Customer c = order.getCustomer();
            csv.append(csvCell(order.getOrderNumber())).append(',')
                    .append(csvCell(order.getCreatedAt() != null ? order.getCreatedAt().toString() : ""))
                    .append(',')
                    .append(csvCell(c != null ? c.getFullName() : ""))
                    .append(',')
                    .append(csvCell(c != null ? c.getPhone() : ""))
                    .append(',')
                    .append(csvCell(c != null ? c.getCity() : ""))
                    .append(',')
                    .append(csvCell(order.getStatus()))
                    .append(',')
                    .append(csvCell(order.getPaymentMethod()))
                    .append(',')
                    .append(order.getTotalAmount() != null ? order.getTotalAmount() : 0)
                    .append('\n');
        }
        return csv.toString();
    }

    @Transactional(readOnly = true)
    public String metaCatalogCsv() {
        Long fid = requireFid();
        String slug = fournisseurRepository.findById(fid).map(Fournisseur::getSlug).orElse("");
        String origin = publicSiteUrl.endsWith("/") ? publicSiteUrl.substring(0, publicSiteUrl.length() - 1) : publicSiteUrl;
        StringBuilder csv = new StringBuilder();
        csv.append("id,title,description,availability,condition,price,link,image_link,brand\n");
        for (Product p : productRepository.findAllWithImages(PageRequest.of(0, 500)).getContent()) {
            String image = "";
            if (p.getImages() != null && !p.getImages().isEmpty() && p.getImages().get(0).getUrl() != null) {
                image = absolute(origin, p.getImages().get(0).getUrl());
            }
            String desc = p.getShortDescription() != null && !p.getShortDescription().isBlank()
                    ? p.getShortDescription() : p.getDescription();
            String link = origin + "/produit/" + p.getId() + (slug.isBlank() ? "" : "?tenant=" + slug);
            boolean inStock = p.getStock() != null && p.getStock() > 0;
            csv.append(p.getId()).append(',')
                    .append(csvCell(p.getName())).append(',')
                    .append(csvCell(desc)).append(',')
                    .append(inStock ? "in stock" : "out of stock").append(',')
                    .append("new,")
                    .append(String.format(Locale.US, "%.2f MAD", p.getPrice() != null ? p.getPrice() : 0)).append(',')
                    .append(csvCell(link)).append(',')
                    .append(csvCell(image)).append(',')
                    .append(csvCell(p.getMarque()))
                    .append('\n');
        }
        return csv.toString();
    }

    private ReferralCode findActiveReferral(String code) {
        if (code == null || code.isBlank()) {
            throw new BusinessException("Code parrain requis", HttpStatus.BAD_REQUEST);
        }
        return referralCodeRepository.findByFournisseurIdAndCodeIgnoreCase(requireFid(), code.trim())
                .filter(ReferralCode::isActive)
                .orElseThrow(() -> new BusinessException("Code parrain invalide", HttpStatus.BAD_REQUEST));
    }

    private Map<String, Object> campaignMap(SeasonalCampaign c) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("code", c.getCode());
        m.put("title", c.getTitle());
        m.put("discountPercent", c.getDiscountPercent());
        m.put("startsOn", c.getStartsOn());
        m.put("endsOn", c.getEndsOn());
        return m;
    }

    private StoreSettings currentSettings() {
        Long fid = TenantContext.getFournisseurId();
        if (fid == null) return null;
        return storeSettingsRepository.findByFournisseurId(fid).orElse(null);
    }

    private StoreSettings requireSettings() {
        return storeSettingsRepository.findByFournisseurId(requireFid())
                .orElseThrow(() -> new BusinessException("Paramètres boutique introuvables", HttpStatus.NOT_FOUND));
    }

    private Long requireFid() {
        return TenantContext.requireFournisseurId();
    }

    private static String digits(String raw) {
        return raw == null ? "" : raw.replaceAll("\\D", "");
    }

    private static String csvCell(String raw) {
        String v = raw == null ? "" : raw.replace("\r", " ").replace("\n", " ");
        if (v.contains(",") || v.contains("\"")) {
            return "\"" + v.replace("\"", "\"\"") + "\"";
        }
        return v;
    }

    private static String absolute(String origin, String url) {
        if (url.startsWith("http://") || url.startsWith("https://")) return url;
        return origin + (url.startsWith("/") ? url : "/" + url);
    }
}
