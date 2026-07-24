package ma.codexa.troco.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ma.codexa.troco.common.exception.ResourceNotFoundException;
import ma.codexa.troco.dto.*;
import ma.codexa.troco.entity.AutoPromoRule;
import ma.codexa.troco.entity.PromoCode;
import ma.codexa.troco.repository.AutoPromoRuleRepository;
import ma.codexa.troco.repository.PromoCodeRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class PromoCodeService {

    private final PromoCodeRepository promoCodeRepository;
    private final AutoPromoRuleRepository autoPromoRuleRepository;

    private static final String CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int CODE_LENGTH = 6;

    // ═══════════════════════════════════════════════════════════
    // Promo Codes CRUD
    // ═══════════════════════════════════════════════════════════

    @Transactional(readOnly = true)
    public List<PromoCodeDTO> getAllPromoCodes() {
        return promoCodeRepository.findAll().stream().map(this::toDTO).toList();
    }

    @Transactional(readOnly = true)
    public Page<PromoCodeListItemDTO> getPromoCodesPage(String keyword, Pageable pageable) {
        boolean applyKeywordFilter = keyword != null && !keyword.isBlank();
        String keywordPattern = applyKeywordFilter ? "%" + keyword.trim().toLowerCase() + "%" : "%";
        return promoCodeRepository.findWithFilters(applyKeywordFilter, keywordPattern, pageable)
                .map(this::toListItemDTO);
    }

    @Transactional(readOnly = true)
    public PromoCodeStatsDTO getPromoCodeStats() {
        return new PromoCodeStatsDTO(
                promoCodeRepository.count(),
                promoCodeRepository.countByIsActiveTrue(),
                promoCodeRepository.countByType("single_use"),
                promoCodeRepository.countByType("reusable")
        );
    }

    @Transactional(readOnly = true)
    public PromoCodeDTO getPromoCodeById(Long id) {
        PromoCode pc = promoCodeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Code promo", id));
        return toDTO(pc);
    }

    public PromoCodeDTO createPromoCode(CreatePromoCodeRequest req) {
        if (promoCodeRepository.existsByCodeIgnoreCase(req.getCode())) {
            throw new IllegalArgumentException("Ce code promo existe déjà");
        }
        PromoCode pc = new PromoCode();
        pc.setCode(req.getCode().toUpperCase().trim());
        pc.setType(req.getType());
        pc.setDiscountType(req.getDiscountType());
        pc.setDiscountValue(req.getDiscountValue());
        pc.setMinOrderAmount(req.getMinOrderAmount());
        pc.setMaxUses("single_use".equals(req.getType()) ? Integer.valueOf(1) : req.getMaxUses());
        pc.setIsActive(req.getIsActive() != null ? req.getIsActive() : true);
        if (req.getExpiresAt() != null && !req.getExpiresAt().isBlank()) {
            pc.setExpiresAt(LocalDateTime.parse(req.getExpiresAt(), DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        }
        PromoCode saved = promoCodeRepository.save(pc);
        log.info("Promo code created: {} type={} discount={}{}",
                saved.getCode(), saved.getType(), saved.getDiscountValue(),
                "percentage".equals(saved.getDiscountType()) ? "%" : " DH");
        return toDTO(saved);
    }

    public PromoCodeDTO updatePromoCode(Long id, CreatePromoCodeRequest req) {
        PromoCode pc = promoCodeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Code promo", id));
        if (!pc.getCode().equalsIgnoreCase(req.getCode())
                && promoCodeRepository.existsByCodeIgnoreCase(req.getCode())) {
            throw new IllegalArgumentException("Ce code promo existe déjà");
        }
        pc.setCode(req.getCode().toUpperCase().trim());
        pc.setType(req.getType());
        pc.setDiscountType(req.getDiscountType());
        pc.setDiscountValue(req.getDiscountValue());
        pc.setMinOrderAmount(req.getMinOrderAmount());
        pc.setMaxUses("single_use".equals(req.getType()) ? Integer.valueOf(1) : req.getMaxUses());
        pc.setIsActive(req.getIsActive() != null ? req.getIsActive() : true);
        if (req.getExpiresAt() != null && !req.getExpiresAt().isBlank()) {
            pc.setExpiresAt(LocalDateTime.parse(req.getExpiresAt(), DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        } else {
            pc.setExpiresAt(null);
        }
        return toDTO(promoCodeRepository.save(pc));
    }

    public void deletePromoCode(Long id) {
        PromoCode pc = promoCodeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Code promo", id));
        promoCodeRepository.delete(pc);
    }

    public PromoCodeDTO toggleActive(Long id, boolean isActive) {
        PromoCode pc = promoCodeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Code promo", id));
        pc.setIsActive(isActive);
        return toDTO(promoCodeRepository.save(pc));
    }

    public String generateCode() {
        String code;
        do {
            code = randomCode();
        } while (promoCodeRepository.existsByCodeIgnoreCase(code));
        return code;
    }

    // ═══════════════════════════════════════════════════════════
    // Public: Validate & use
    // ═══════════════════════════════════════════════════════════

    @Transactional(readOnly = true)
    public ValidatePromoCodeResponse validateCode(String code, double orderTotal) {
        var opt = promoCodeRepository.findByCodeIgnoreCase(code.trim());
        if (opt.isEmpty()) {
            return ValidatePromoCodeResponse.invalid("Code promo introuvable");
        }
        PromoCode pc = opt.get();
        if (!pc.getIsActive()) return ValidatePromoCodeResponse.invalid("Ce code promo est désactivé");
        if (pc.isExpired()) return ValidatePromoCodeResponse.invalid("Ce code promo a expiré");
        if (pc.hasReachedMaxUses()) return ValidatePromoCodeResponse.invalid("Ce code promo a atteint le nombre maximum d'utilisations");
        if (pc.getMinOrderAmount() != null && orderTotal < pc.getMinOrderAmount()) {
            return ValidatePromoCodeResponse.invalid(
                    "Commande minimum de " + pc.getMinOrderAmount().intValue() + " DH requise");
        }
        return ValidatePromoCodeResponse.valid(pc.getCode(), pc.getDiscountType(), pc.getDiscountValue());
    }

    /**
     * Called when an order is placed with a promo code — increments usage counter.
     */
    public void usePromoCode(String code) {
        promoCodeRepository.findByCodeIgnoreCase(code.trim()).ifPresent(pc -> {
            pc.incrementUses();
            promoCodeRepository.save(pc);
            log.info("Promo code used: {} (uses={}/{})", pc.getCode(), pc.getCurrentUses(),
                    pc.getMaxUses() != null ? pc.getMaxUses() : "∞");
        });
    }

    // ═══════════════════════════════════════════════════════════
    // Auto Promo Rules CRUD
    // ═══════════════════════════════════════════════════════════

    @Transactional(readOnly = true)
    public List<AutoPromoRuleDTO> getAllRules() {
        return autoPromoRuleRepository.findAll().stream().map(this::toRuleDTO).toList();
    }

    public AutoPromoRuleDTO createRule(CreateAutoPromoRuleRequest req) {
        AutoPromoRule rule = new AutoPromoRule();
        rule.setMinOrderAmount(req.getMinOrderAmount());
        rule.setDiscountType(req.getDiscountType());
        rule.setDiscountValue(req.getDiscountValue());
        rule.setIsActive(req.getIsActive() != null ? req.getIsActive() : true);
        return toRuleDTO(autoPromoRuleRepository.save(rule));
    }

    public AutoPromoRuleDTO updateRule(Long id, CreateAutoPromoRuleRequest req) {
        AutoPromoRule rule = autoPromoRuleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Règle auto promo", id));
        rule.setMinOrderAmount(req.getMinOrderAmount());
        rule.setDiscountType(req.getDiscountType());
        rule.setDiscountValue(req.getDiscountValue());
        rule.setIsActive(req.getIsActive() != null ? req.getIsActive() : true);
        return toRuleDTO(autoPromoRuleRepository.save(rule));
    }

    public void deleteRule(Long id) {
        AutoPromoRule rule = autoPromoRuleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Règle auto promo", id));
        autoPromoRuleRepository.delete(rule);
    }

    /**
     * Check if an order total qualifies for an auto-generated promo code.
     * Returns the best matching rule (highest minOrderAmount that the total exceeds).
     * Generates a single-use promo code on the fly and saves it.
     */
    public AutoPromoCheckResponse checkAutoPromo(double orderTotal) {
        List<AutoPromoRule> rules = autoPromoRuleRepository.findByIsActiveTrueOrderByMinOrderAmountDesc();
        for (AutoPromoRule rule : rules) {
            if (orderTotal >= rule.getMinOrderAmount()) {
                String code = generateCode();
                // Create a single-use promo code from this rule
                PromoCode pc = new PromoCode();
                pc.setCode(code);
                pc.setType("single_use");
                pc.setDiscountType(rule.getDiscountType());
                pc.setDiscountValue(rule.getDiscountValue());
                pc.setIsActive(true);
                pc.setMaxUses(1);
                promoCodeRepository.save(pc);
                log.info("Auto promo code generated: {} for rule id={} (min={} DH)",
                        code, rule.getId(), rule.getMinOrderAmount());
                return AutoPromoCheckResponse.eligible(toRuleDTO(rule), code);
            }
        }
        return AutoPromoCheckResponse.notEligible();
    }

    // ═══════════════════════════════════════════════════════════
    // Public: Suggestions & public codes
    // ═══════════════════════════════════════════════════════════

    @Transactional(readOnly = true)
    public List<PromoSuggestionDTO> getSuggestions(double orderTotal) {
        List<PromoCode> allActive = promoCodeRepository.findAll().stream()
                .filter(pc -> pc.getIsActive()
                        && !pc.isExpired()
                        && !pc.hasReachedMaxUses()
                        && pc.getMinOrderAmount() != null
                        && pc.getMinOrderAmount() > 0)
                .sorted(java.util.Comparator.comparingDouble(PromoCode::getMinOrderAmount))
                .toList();

        List<PromoSuggestionDTO> suggestions = new java.util.ArrayList<>();

        for (PromoCode pc : allActive) {
            double minAmount = pc.getMinOrderAmount();
            boolean qualified = orderTotal >= minAmount;
            double amountNeeded = qualified ? 0 : minAmount - orderTotal;

            suggestions.add(new PromoSuggestionDTO(
                    pc.getCode(),
                    pc.getDiscountType(),
                    pc.getDiscountValue(),
                    minAmount,
                    amountNeeded,
                    qualified
            ));
        }

        List<PromoSuggestionDTO> qualifiedList = suggestions.stream().filter(PromoSuggestionDTO::isQualified).toList();
        List<PromoSuggestionDTO> notYet = suggestions.stream().filter(s -> !s.isQualified()).toList();

        List<PromoSuggestionDTO> result = new java.util.ArrayList<>(qualifiedList);
        int remaining = 5 - result.size();
        if (remaining > 0 && !notYet.isEmpty()) {
            result.addAll(notYet.subList(0, Math.min(remaining, notYet.size())));
        }

        if (result.size() > 5) {
            result = result.subList(result.size() - 5, result.size());
        }

        return result;
    }

    @Transactional(readOnly = true)
    public List<PublicPromoCodeDTO> getPublicCodes() {
        return promoCodeRepository.findAll().stream()
                .filter(pc -> pc.getIsActive()
                        && !pc.isExpired()
                        && !pc.hasReachedMaxUses()
                        && pc.getMinOrderAmount() != null
                        && pc.getMinOrderAmount() > 0)
                .sorted(java.util.Comparator.comparingDouble(PromoCode::getMinOrderAmount))
                .map(pc -> new PublicPromoCodeDTO(
                        pc.getCode(),
                        pc.getDiscountType(),
                        pc.getDiscountValue(),
                        pc.getMinOrderAmount(),
                        pc.getExpiresAt() != null ? pc.getExpiresAt().toString() : null
                ))
                .toList();
    }

    // ═══════════════════════════════════════════════════════════
    // Mapping helpers
    // ═══════════════════════════════════════════════════════════

    private PromoCodeDTO toDTO(PromoCode pc) {
        PromoCodeDTO dto = new PromoCodeDTO();
        dto.setId(pc.getId());
        dto.setCode(pc.getCode());
        dto.setType(pc.getType());
        dto.setDiscountType(pc.getDiscountType());
        dto.setDiscountValue(pc.getDiscountValue());
        dto.setMinOrderAmount(pc.getMinOrderAmount());
        dto.setMaxUses(pc.getMaxUses());
        dto.setCurrentUses(pc.getCurrentUses());
        dto.setIsActive(pc.getIsActive());
        dto.setExpiresAt(pc.getExpiresAt() != null ? pc.getExpiresAt().toString() : null);
        dto.setCreatedAt(pc.getCreatedAt() != null ? pc.getCreatedAt().toString() : null);
        return dto;
    }

    private PromoCodeListItemDTO toListItemDTO(PromoCode pc) {
        PromoCodeListItemDTO dto = new PromoCodeListItemDTO();
        dto.setId(pc.getId());
        dto.setCode(pc.getCode());
        dto.setType(pc.getType());
        dto.setDiscountType(pc.getDiscountType());
        dto.setDiscountValue(pc.getDiscountValue());
        dto.setMinOrderAmount(pc.getMinOrderAmount());
        dto.setMaxUses(pc.getMaxUses());
        dto.setCurrentUses(pc.getCurrentUses());
        dto.setIsActive(pc.getIsActive());
        dto.setExpiresAt(pc.getExpiresAt() != null ? pc.getExpiresAt().toString() : null);
        return dto;
    }

    private AutoPromoRuleDTO toRuleDTO(AutoPromoRule rule) {
        AutoPromoRuleDTO dto = new AutoPromoRuleDTO();
        dto.setId(rule.getId());
        dto.setMinOrderAmount(rule.getMinOrderAmount());
        dto.setDiscountType(rule.getDiscountType());
        dto.setDiscountValue(rule.getDiscountValue());
        dto.setIsActive(rule.getIsActive());
        dto.setCreatedAt(rule.getCreatedAt() != null ? rule.getCreatedAt().toString() : null);
        return dto;
    }

    private String randomCode() {
        StringBuilder sb = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            sb.append(CODE_CHARS.charAt(ThreadLocalRandom.current().nextInt(CODE_CHARS.length())));
        }
        return sb.toString();
    }
}
