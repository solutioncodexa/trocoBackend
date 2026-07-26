package ma.codexa.troco.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ma.codexa.troco.common.exception.BusinessException;
import ma.codexa.troco.config.CmiProperties;
import ma.codexa.troco.dto.BillingResultDTO;
import ma.codexa.troco.dto.CmiCheckoutDTO;
import ma.codexa.troco.entity.Fournisseur;
import ma.codexa.troco.entity.Plan;
import ma.codexa.troco.entity.SubscriptionPayment;
import ma.codexa.troco.repository.FournisseurRepository;
import ma.codexa.troco.repository.PlanRepository;
import ma.codexa.troco.repository.SubscriptionPaymentRepository;
import ma.codexa.troco.tenant.FournisseurStatus;
import ma.codexa.troco.tenant.TenantContext;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

/**
 * Paiement d'abonnement boutique via CMI (NestPay 3D Pay Hosting).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CmiBillingService {

    private final CmiProperties cmi;
    private final FournisseurRepository fournisseurRepository;
    private final PlanRepository planRepository;
    private final SubscriptionPaymentRepository paymentRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Paiement simulé pour les tests (aucun appel CMI).
     * Activé par défaut via {@code app.cmi.test-pass=true}.
     */
    @Transactional
    public BillingResultDTO simulateTestPayment(String planCode) {
        if (!cmi.testPass()) {
            throw new BusinessException(
                    "Paiement test désactivé. Activez app.cmi.test-pass=true ou configurez CMI.",
                    HttpStatus.SERVICE_UNAVAILABLE);
        }
        ResolvedPlan resolved = resolvePlanForCurrentTenant(planCode);
        String oid = "TEST-" + resolved.fid() + "-"
                + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase(Locale.ROOT);

        SubscriptionPayment payment = new SubscriptionPayment();
        payment.setFournisseurId(resolved.fid());
        payment.setPlanId(resolved.plan().getId());
        payment.setOid(oid);
        payment.setAmountMad(resolved.plan().getPriceMad());
        payment.setCurrency("MAD");
        payment.setStatus("PAID");
        payment.setPaidAt(LocalDateTime.now());
        payment.setCmiResponse("{\"mode\":\"test_pass\"}");
        paymentRepository.save(payment);
        activateSubscription(payment);

        Fournisseur f = fournisseurRepository.findById(resolved.fid()).orElseThrow();
        log.info("billing_test_pass oid={} fournisseurId={} plan={}", oid, resolved.fid(), resolved.plan().getCode());
        return new BillingResultDTO(
                "test_pass",
                oid,
                "PAID",
                resolved.plan().getCode(),
                resolved.plan().getName(),
                resolved.plan().getPriceMad(),
                f.getSubscriptionEndsAt() != null ? f.getSubscriptionEndsAt().toString() : null
        );
    }

    @Transactional
    public CmiCheckoutDTO initiatePlanCheckout(String planCode) {
        if (cmi.testPass() || !cmi.enabled()) {
            throw new BusinessException(
                    "Mode test actif : utilisez POST /billing/test-pass (pas de gateway CMI).",
                    HttpStatus.BAD_REQUEST);
        }
        if (blank(cmi.clientId()) || blank(cmi.storeKey()) || blank(cmi.gatewayUrl())) {
            throw new BusinessException("Configuration CMI incomplète", HttpStatus.SERVICE_UNAVAILABLE);
        }

        ResolvedPlan resolved = resolvePlanForCurrentTenant(planCode);
        Long fid = resolved.fid();
        Plan plan = resolved.plan();

        String oid = "MAT-" + fid + "-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase(Locale.ROOT);
        String amount = plan.getPriceMad().setScale(2, RoundingMode.HALF_UP).toPlainString();
        String rnd = String.valueOf(System.currentTimeMillis());

        SubscriptionPayment payment = new SubscriptionPayment();
        payment.setFournisseurId(fid);
        payment.setPlanId(plan.getId());
        payment.setOid(oid);
        payment.setAmountMad(plan.getPriceMad());
        payment.setCurrency("MAD");
        payment.setStatus("PENDING");
        paymentRepository.save(payment);

        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("clientid", cmi.clientId());
        fields.put("amount", amount);
        fields.put("oid", oid);
        fields.put("okUrl", cmi.okUrl());
        fields.put("failUrl", cmi.failUrl());
        fields.put("callbackUrl", cmi.callbackUrl());
        fields.put("rnd", rnd);
        fields.put("currency", cmi.currency());
        fields.put("storetype", cmi.storeType());
        fields.put("lang", cmi.lang());
        fields.put("hashAlgorithm", cmi.hashAlgorithm());
        fields.put("TranType", "PreAuth");
        fields.put("refreshtime", "5");
        fields.put("encoding", "UTF-8");
        fields.put("HASH", computeHash(fields, cmi.storeKey()));

        log.info("cmi_checkout_initiated fournisseurId={} plan={} oid={} amount={}", fid, plan.getCode(), oid, amount);
        return new CmiCheckoutDTO(cmi.gatewayUrl(), oid, fields);
    }

    /**
     * Callback serveur CMI (APPROVED / DECLINED). Répondre "APPROVED" ou "DECLINED" en texte brut.
     */
    @Transactional
    public String handleCallback(Map<String, String> params) {
        String oid = first(params, "oid", "ReturnOid");
        String procReturnCode = first(params, "ProcReturnCode", "procreturncode");
        if (oid == null || oid.isBlank()) {
            log.warn("cmi_callback_missing_oid");
            return "DECLINED";
        }

        SubscriptionPayment payment = paymentRepository.findByOid(oid).orElse(null);
        if (payment == null) {
            log.warn("cmi_callback_unknown_oid oid={}", oid);
            return "DECLINED";
        }

        try {
            payment.setCmiResponse(objectMapper.writeValueAsString(params));
        } catch (Exception e) {
            payment.setCmiResponse(String.valueOf(params));
        }

        boolean approved = "00".equals(procReturnCode);
        if (approved && verifyHash(params)) {
            payment.setStatus("PAID");
            payment.setPaidAt(LocalDateTime.now());
            paymentRepository.save(payment);
            activateSubscription(payment);
            log.info("cmi_payment_paid oid={} fournisseurId={}", oid, payment.getFournisseurId());
            return "APPROVED";
        }

        payment.setStatus("FAILED");
        paymentRepository.save(payment);
        log.warn("cmi_payment_failed oid={} procReturnCode={}", oid, procReturnCode);
        return "DECLINED";
    }

    @Transactional
    public void markReturnPaidIfApproved(Map<String, String> params) {
        String oid = first(params, "oid", "ReturnOid");
        String procReturnCode = first(params, "ProcReturnCode", "procreturncode");
        if (oid == null || !"00".equals(procReturnCode)) {
            return;
        }
        paymentRepository.findByOid(oid).ifPresent(payment -> {
            if ("PAID".equals(payment.getStatus())) {
                return;
            }
            if (!verifyHash(params)) {
                return;
            }
            payment.setStatus("PAID");
            payment.setPaidAt(LocalDateTime.now());
            try {
                payment.setCmiResponse(objectMapper.writeValueAsString(params));
            } catch (Exception ignored) {
                /* keep previous */
            }
            paymentRepository.save(payment);
            activateSubscription(payment);
        });
    }

    private record ResolvedPlan(Long fid, Plan plan) {}

    private ResolvedPlan resolvePlanForCurrentTenant(String planCode) {
        Long fid = TenantContext.requireFournisseurId();
        Fournisseur f = fournisseurRepository.findById(fid)
                .orElseThrow(() -> new BusinessException("Boutique introuvable", HttpStatus.NOT_FOUND));
        String code = planCode != null && !planCode.isBlank() ? planCode.trim() : null;
        Plan plan = code != null
                ? planRepository.findByCodeIgnoreCase(code)
                    .orElseThrow(() -> new BusinessException("Plan introuvable", HttpStatus.BAD_REQUEST))
                : f.getPlan();
        if (plan == null || !plan.isActive()) {
            throw new BusinessException("Aucun plan actif à régler", HttpStatus.BAD_REQUEST);
        }
        if (plan.getPriceMad() == null || plan.getPriceMad().compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("Montant de plan invalide", HttpStatus.BAD_REQUEST);
        }
        return new ResolvedPlan(fid, plan);
    }

    private void activateSubscription(SubscriptionPayment payment) {
        Fournisseur f = fournisseurRepository.findById(payment.getFournisseurId()).orElse(null);
        Plan plan = planRepository.findById(payment.getPlanId()).orElse(null);
        if (f == null || plan == null) {
            return;
        }
        f.setPlan(plan);
        // Prolonge l'abonnement ; l'activation publique reste du ressort du Super Admin (PENDING).
        f.setSubscriptionEndsAt(LocalDateTime.now().plusMonths(1));
        if (FournisseurStatus.CANCELLED.equalsIgnoreCase(f.getStatus())
                || FournisseurStatus.TRIAL.equalsIgnoreCase(f.getStatus())) {
            f.setStatus(FournisseurStatus.ACTIVE);
        }
        fournisseurRepository.save(f);
    }

    /** Hash NestPay ver3 : champs triés alpha, exclus hash/encoding/countdown, + storeKey. */
    static String computeHash(Map<String, String> fields, String storeKey) {
        TreeMap<String, String> sorted = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        sorted.putAll(fields);
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> e : sorted.entrySet()) {
            String k = e.getKey();
            if (k == null) continue;
            String lower = k.toLowerCase(Locale.ROOT);
            if ("hash".equals(lower) || "encoding".equals(lower) || "countdown".equals(lower)) {
                continue;
            }
            String v = e.getValue() == null ? "" : e.getValue().replace("|", "\\|").replace("\\", "\\\\");
            if (!sb.isEmpty()) sb.append('|');
            sb.append(v);
        }
        sb.append('|').append(storeKey);
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-512");
            byte[] digest = md.digest(sb.toString().getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(digest);
        } catch (Exception e) {
            throw new IllegalStateException("CMI hash error", e);
        }
    }

    private boolean verifyHash(Map<String, String> params) {
        String received = first(params, "HASH", "hash");
        if (received == null || blank(cmi.storeKey())) {
            return false;
        }
        Map<String, String> copy = new LinkedHashMap<>(params);
        copy.remove("HASH");
        copy.remove("hash");
        String expected = computeHash(copy, cmi.storeKey());
        return expected.equals(received);
    }

    private static String first(Map<String, String> map, String... keys) {
        for (String k : keys) {
            if (map.containsKey(k) && map.get(k) != null && !map.get(k).isBlank()) {
                return map.get(k);
            }
        }
        return null;
    }

    private static boolean blank(String s) {
        return s == null || s.isBlank();
    }
}
