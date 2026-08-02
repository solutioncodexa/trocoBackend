package ma.codexa.troco.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.Balance;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ma.codexa.troco.common.exception.BusinessException;
import ma.codexa.troco.config.CmiProperties;
import ma.codexa.troco.dto.CmiCheckoutDTO;
import ma.codexa.troco.dto.StorePaymentsConfigDTO;
import ma.codexa.troco.dto.request.PaymentCredentialsTestRequest;
import ma.codexa.troco.dto.request.StoreCmiInitRequest;
import ma.codexa.troco.dto.request.StorePayPalCreateOrderRequest;
import ma.codexa.troco.dto.request.StoreStripeChargeRequest;
import ma.codexa.troco.dto.request.StoreStripePaymentIntentRequest;
import ma.codexa.troco.entity.Fournisseur;
import ma.codexa.troco.entity.StoreSettings;
import ma.codexa.troco.repository.FournisseurRepository;
import ma.codexa.troco.repository.StoreSettingsRepository;
import ma.codexa.troco.tenant.TenantContext;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class StorePaymentGatewayService {

    private final StoreSettingsRepository storeSettingsRepository;
    private final FournisseurRepository fournisseurRepository;
    private final CmiProperties cmiProperties;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RestTemplate restTemplate = new RestTemplate();

    public static boolean isStripeReady(StoreSettings s) {
        return s != null
                && Boolean.TRUE.equals(s.getPaymentStripeEnabled())
                && notBlank(s.getStripePublishableKey())
                && notBlank(s.getStripeSecretKey())
                && s.getStripeVerifiedAt() != null;
    }

    public static boolean isPaypalReady(StoreSettings s) {
        return s != null
                && Boolean.TRUE.equals(s.getPaymentPaypalEnabled())
                && notBlank(s.getPaypalClientId())
                && notBlank(s.getPaypalClientSecret())
                && s.getPaypalVerifiedAt() != null;
    }

    public static boolean isCmiReady(StoreSettings s) {
        return s != null
                && Boolean.TRUE.equals(s.getPaymentCmiEnabled())
                && notBlank(s.getCmiClientId())
                && notBlank(s.getCmiStoreKey())
                && s.getCmiVerifiedAt() != null;
    }

    public StorePaymentsConfigDTO getPublicConfig(String slugOrNull) {
        ResolvedStore store = resolvePublicStore(slugOrNull);
        StoreSettings s = store.settings();
        boolean stripeReady = isStripeReady(s);
        boolean paypalReady = isPaypalReady(s);
        boolean cmiReady = isCmiReady(s);
        return new StorePaymentsConfigDTO(
                store.fournisseur().getSlug(),
                !Boolean.FALSE.equals(s.getPaymentCodEnabled()),
                stripeReady,
                paypalReady,
                cmiReady,
                Boolean.TRUE.equals(s.getPaymentBnplEnabled()),
                s.getBnplProvider(),
                stripeReady ? s.getStripePublishableKey() : null,
                paypalReady ? s.getPaypalClientId() : null,
                paypalReady ? FournisseurService.normalizePaypalMode(s.getPaypalMode()) : null
        );
    }

    @Transactional
    public String testStripe(PaymentCredentialsTestRequest request) {
        StoreSettings settings = requireMySettings();
        mergeTestCredentials(settings, request);
        String secret = firstNonBlank(
                request != null ? request.getStripeSecretKey() : null,
                settings.getStripeSecretKey());
        String publishable = firstNonBlank(
                request != null ? request.getStripePublishableKey() : null,
                settings.getStripePublishableKey());
        if (!notBlank(secret) || !notBlank(publishable)) {
            throw new BusinessException("Clés Stripe incomplètes (publishable + secret)", HttpStatus.BAD_REQUEST);
        }
        try {
            Stripe.apiKey = secret;
            Balance.retrieve();
        } catch (StripeException e) {
            settings.setStripeVerifiedAt(null);
            storeSettingsRepository.save(settings);
            throw new BusinessException("Connexion Stripe échouée : " + e.getMessage(), HttpStatus.BAD_REQUEST);
        }
        settings.setStripePublishableKey(publishable.trim());
        settings.setStripeSecretKey(secret.trim());
        settings.setPaymentStripeEnabled(true);
        settings.setStripeVerifiedAt(LocalDateTime.now());
        storeSettingsRepository.save(settings);
        log.info("stripe_verified fournisseurId={}", settings.getFournisseurId());
        return "Stripe connecté — paiement activé pour votre boutique.";
    }

    @Transactional
    public String testPaypal(PaymentCredentialsTestRequest request) {
        StoreSettings settings = requireMySettings();
        mergeTestCredentials(settings, request);
        String clientId = firstNonBlank(
                request != null ? request.getPaypalClientId() : null,
                settings.getPaypalClientId());
        String clientSecret = firstNonBlank(
                request != null ? request.getPaypalClientSecret() : null,
                settings.getPaypalClientSecret());
        String mode = FournisseurService.normalizePaypalMode(
                firstNonBlank(request != null ? request.getPaypalMode() : null, settings.getPaypalMode()));
        if (!notBlank(clientId) || !notBlank(clientSecret)) {
            throw new BusinessException("Clés PayPal incomplètes (client id + secret)", HttpStatus.BAD_REQUEST);
        }
        try {
            fetchPayPalAccessToken(clientId.trim(), clientSecret.trim(), mode);
        } catch (BusinessException e) {
            settings.setPaypalVerifiedAt(null);
            storeSettingsRepository.save(settings);
            throw e;
        } catch (Exception e) {
            settings.setPaypalVerifiedAt(null);
            storeSettingsRepository.save(settings);
            throw new BusinessException("Connexion PayPal échouée : " + e.getMessage(), HttpStatus.BAD_REQUEST);
        }
        settings.setPaypalClientId(clientId.trim());
        settings.setPaypalClientSecret(clientSecret.trim());
        settings.setPaypalMode(mode);
        settings.setPaymentPaypalEnabled(true);
        settings.setPaypalVerifiedAt(LocalDateTime.now());
        storeSettingsRepository.save(settings);
        log.info("paypal_verified fournisseurId={} mode={}", settings.getFournisseurId(), mode);
        return "PayPal connecté (" + mode + ") — paiement activé pour votre boutique.";
    }

    @Transactional
    public String testCmi(PaymentCredentialsTestRequest request) {
        StoreSettings settings = requireMySettings();
        mergeTestCredentials(settings, request);
        String clientId = firstNonBlank(
                request != null ? request.getCmiClientId() : null,
                settings.getCmiClientId());
        String storeKey = firstNonBlank(
                request != null ? request.getCmiStoreKey() : null,
                settings.getCmiStoreKey());
        if (!notBlank(clientId) || !notBlank(storeKey)) {
            throw new BusinessException("Clés CMI incomplètes (client id + store key)", HttpStatus.BAD_REQUEST);
        }
        if (clientId.trim().length() < 4 || storeKey.trim().length() < 8) {
            throw new BusinessException("Clés CMI invalides (format trop court)", HttpStatus.BAD_REQUEST);
        }
        // Validation locale NestPay : le hash doit pouvoir être calculé avec la store key.
        try {
            Map<String, String> probe = new LinkedHashMap<>();
            probe.put("clientid", clientId.trim());
            probe.put("amount", "1.00");
            probe.put("oid", "TEST-" + UUID.randomUUID().toString().substring(0, 8));
            CmiBillingService.computeHash(probe, storeKey.trim());
        } catch (Exception e) {
            settings.setCmiVerifiedAt(null);
            storeSettingsRepository.save(settings);
            throw new BusinessException("Clés CMI invalides : " + e.getMessage(), HttpStatus.BAD_REQUEST);
        }
        settings.setCmiClientId(clientId.trim());
        settings.setCmiStoreKey(storeKey.trim());
        settings.setPaymentCmiEnabled(true);
        settings.setCmiVerifiedAt(LocalDateTime.now());
        storeSettingsRepository.save(settings);
        log.info("cmi_verified fournisseurId={}", settings.getFournisseurId());
        return "CMI connecté — paiement activé pour votre boutique.";
    }

    public Map<String, String> createStripePaymentIntent(StoreStripePaymentIntentRequest request) {
        StoreSettings settings = requireReadyStoreSettings("stripe");
        try {
            Stripe.apiKey = settings.getStripeSecretKey();
            String currency = request.getCurrency() != null
                    ? request.getCurrency().trim().toLowerCase(Locale.ROOT)
                    : "mad";
            PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                    .setAmount(request.getAmountCents())
                    .setCurrency(currency)
                    .setDescription(request.getDescription() != null ? request.getDescription() : "Commande boutique")
                    .setAutomaticPaymentMethods(
                            PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                                    .setEnabled(true)
                                    .build())
                    .build();
            PaymentIntent intent = PaymentIntent.create(params);
            return Map.of(
                    "paymentIntentId", intent.getId(),
                    "clientSecret", intent.getClientSecret()
            );
        } catch (StripeException e) {
            throw new BusinessException("Stripe : " + e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * Débit carte comme Grammar : PaymentMethod (Elements) → PaymentIntent confirmé serveur.
     */
    public Map<String, String> chargeStripePaymentMethod(StoreStripeChargeRequest request) {
        StoreSettings settings = requireReadyStoreSettings("stripe");
        try {
            Stripe.apiKey = settings.getStripeSecretKey();
            String currency = request.getCurrency() != null
                    ? request.getCurrency().trim().toLowerCase(Locale.ROOT)
                    : "mad";
            PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                    .setAmount(request.getAmountCents())
                    .setCurrency(currency)
                    .setPaymentMethod(request.getPaymentMethodId().trim())
                    .addPaymentMethodType("card")
                    .setConfirm(true)
                    .setDescription(request.getDescription() != null
                            ? request.getDescription()
                            : "Commande boutique")
                    .build();
            PaymentIntent intent = PaymentIntent.create(params);
            if (!"succeeded".equals(intent.getStatus())) {
                throw new BusinessException(
                        "Paiement Stripe non finalisé (statut : " + intent.getStatus() + ")",
                        HttpStatus.BAD_REQUEST);
            }
            return Map.of(
                    "paymentIntentId", intent.getId(),
                    "status", intent.getStatus()
            );
        } catch (StripeException e) {
            throw new BusinessException("Stripe : " + e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    public Map<String, String> createPayPalOrder(StorePayPalCreateOrderRequest request) {
        StoreSettings settings = requireReadyStoreSettings("paypal");
        String mode = FournisseurService.normalizePaypalMode(settings.getPaypalMode());
        String token = fetchPayPalAccessToken(
                settings.getPaypalClientId(), settings.getPaypalClientSecret(), mode);
        String currency = request.getCurrency() != null
                ? request.getCurrency().trim().toUpperCase(Locale.ROOT)
                : "MAD";
        String amount = request.getAmount().setScale(2, RoundingMode.HALF_UP).toPlainString();
        String body;
        try {
            body = objectMapper.writeValueAsString(Map.of(
                    "intent", "CAPTURE",
                    "purchase_units", List.of(Map.of(
                            "amount", Map.of(
                                    "currency_code", currency,
                                    "value", amount
                            ),
                            "description", request.getDescription() != null
                                    ? request.getDescription()
                                    : "Commande boutique"
                    ))
            ));
        } catch (Exception e) {
            throw new BusinessException("PayPal : requête invalide", HttpStatus.BAD_REQUEST);
        }
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<String> response = restTemplate.exchange(
                paypalBaseUrl(mode) + "/v2/checkout/orders",
                HttpMethod.POST,
                new HttpEntity<>(body, headers),
                String.class);
        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            throw new BusinessException("PayPal create order failed", HttpStatus.BAD_GATEWAY);
        }
        try {
            JsonNode json = objectMapper.readTree(response.getBody());
            String orderId = json.path("id").asText();
            if (!notBlank(orderId)) {
                throw new BusinessException("PayPal order ID manquant", HttpStatus.BAD_GATEWAY);
            }
            String approveUrl = "";
            if (json.path("links").isArray()) {
                for (JsonNode link : json.path("links")) {
                    if ("approve".equals(link.path("rel").asText())) {
                        approveUrl = link.path("href").asText("");
                        break;
                    }
                }
            }
            Map<String, String> out = new LinkedHashMap<>();
            out.put("orderId", orderId);
            if (notBlank(approveUrl)) out.put("approveUrl", approveUrl);
            return out;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException("PayPal réponse invalide", HttpStatus.BAD_GATEWAY);
        }
    }

    public Map<String, String> capturePayPalOrder(String orderId) {
        StoreSettings settings = requireReadyStoreSettings("paypal");
        String mode = FournisseurService.normalizePaypalMode(settings.getPaypalMode());
        String token = fetchPayPalAccessToken(
                settings.getPaypalClientId(), settings.getPaypalClientSecret(), mode);
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<String> response = restTemplate.exchange(
                paypalBaseUrl(mode) + "/v2/checkout/orders/" + orderId + "/capture",
                HttpMethod.POST,
                new HttpEntity<>("{}", headers),
                String.class);
        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            throw new BusinessException("PayPal capture failed", HttpStatus.BAD_GATEWAY);
        }
        try {
            JsonNode json = objectMapper.readTree(response.getBody());
            String status = json.path("status").asText();
            if (!"COMPLETED".equals(status)) {
                throw new BusinessException("PayPal capture status: " + status, HttpStatus.BAD_REQUEST);
            }
            return Map.of("status", status, "orderId", orderId);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException("PayPal capture réponse invalide", HttpStatus.BAD_GATEWAY);
        }
    }

    public CmiCheckoutDTO initCmiCheckout(StoreCmiInitRequest request) {
        StoreSettings settings = requireReadyStoreSettings("cmi");
        if (blank(cmiProperties.gatewayUrl())) {
            throw new BusinessException(
                    "URL gateway CMI plateforme manquante (app.cmi.gateway-url)",
                    HttpStatus.SERVICE_UNAVAILABLE);
        }
        BigDecimal amount = request.getAmount().setScale(2, RoundingMode.HALF_UP);
        String oid = "ORD-" + settings.getFournisseurId() + "-"
                + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase(Locale.ROOT);
        String rnd = String.valueOf(System.currentTimeMillis());
        String okUrl = firstNonBlank(request.getOkUrl(), cmiProperties.okUrl());
        String failUrl = firstNonBlank(request.getFailUrl(), cmiProperties.failUrl());
        if (!notBlank(okUrl) || !notBlank(failUrl)) {
            throw new BusinessException("URLs retour CMI manquantes", HttpStatus.BAD_REQUEST);
        }

        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("clientid", settings.getCmiClientId());
        fields.put("amount", amount.toPlainString());
        fields.put("oid", oid);
        fields.put("okUrl", okUrl);
        fields.put("failUrl", failUrl);
        if (notBlank(cmiProperties.callbackUrl())) {
            fields.put("callbackUrl", cmiProperties.callbackUrl());
        }
        fields.put("rnd", rnd);
        fields.put("currency", notBlank(cmiProperties.currency()) ? cmiProperties.currency() : "504");
        fields.put("storetype", notBlank(cmiProperties.storeType()) ? cmiProperties.storeType() : "3d_pay_hosting");
        fields.put("lang", notBlank(cmiProperties.lang()) ? cmiProperties.lang() : "fr");
        fields.put("hashAlgorithm", notBlank(cmiProperties.hashAlgorithm()) ? cmiProperties.hashAlgorithm() : "ver3");
        fields.put("TranType", "PreAuth");
        fields.put("refreshtime", "5");
        fields.put("encoding", "UTF-8");
        fields.put("HASH", CmiBillingService.computeHash(fields, settings.getCmiStoreKey()));

        return new CmiCheckoutDTO(cmiProperties.gatewayUrl(), oid, fields);
    }

    public void assertGatewayReadyForOrder(String paymentMethod) {
        if (paymentMethod == null) return;
        String m = paymentMethod.trim().toLowerCase(Locale.ROOT);
        StoreSettings settings = requireCurrentSettingsOrNull();
        if (settings == null) return;
        if (("card_stripe".equals(m) || "stripe".equals(m)) && !isStripeReady(settings)) {
            throw new BusinessException("Stripe n'est pas activé pour cette boutique", HttpStatus.BAD_REQUEST);
        }
        if ("paypal".equals(m) && !isPaypalReady(settings)) {
            throw new BusinessException("PayPal n'est pas activé pour cette boutique", HttpStatus.BAD_REQUEST);
        }
        if (("card_cmi".equals(m) || "online".equals(m)) && !isCmiReady(settings)) {
            throw new BusinessException("CMI n'est pas activé pour cette boutique", HttpStatus.BAD_REQUEST);
        }
    }

    private StoreSettings requireReadyStoreSettings(String gateway) {
        StoreSettings settings = requireCurrentSettingsOrNull();
        if (settings == null) {
            throw new BusinessException("Boutique introuvable", HttpStatus.NOT_FOUND);
        }
        boolean ready = switch (gateway) {
            case "stripe" -> isStripeReady(settings);
            case "paypal" -> isPaypalReady(settings);
            case "cmi" -> isCmiReady(settings);
            default -> false;
        };
        if (!ready) {
            throw new BusinessException(
                    "Paiement " + gateway + " non activé (clés + test requis)",
                    HttpStatus.BAD_REQUEST);
        }
        return settings;
    }

    private StoreSettings requireMySettings() {
        Long fid = TenantContext.requireFournisseurId();
        return storeSettingsRepository.findByFournisseurId(fid)
                .orElseThrow(() -> new BusinessException("Paramètres boutique introuvables", HttpStatus.NOT_FOUND));
    }

    private StoreSettings requireCurrentSettingsOrNull() {
        Long fid = TenantContext.getFournisseurId();
        if (fid == null) return null;
        return storeSettingsRepository.findByFournisseurId(fid).orElse(null);
    }

    private ResolvedStore resolvePublicStore(String slugOrNull) {
        String slug = slugOrNull != null && !slugOrNull.isBlank()
                ? slugOrNull.trim()
                : TenantContext.getSlug();
        if (slug != null && !slug.isBlank()) {
            Fournisseur f = fournisseurRepository.findBySlugIgnoreCase(slug)
                    .orElseThrow(() -> new BusinessException("Boutique introuvable", HttpStatus.NOT_FOUND));
            StoreSettings s = storeSettingsRepository.findByFournisseurId(f.getId())
                    .orElseThrow(() -> new BusinessException("Paramètres introuvables", HttpStatus.NOT_FOUND));
            return new ResolvedStore(f, s);
        }
        Long fid = TenantContext.getFournisseurId();
        if (fid != null) {
            Fournisseur f = fournisseurRepository.findById(fid)
                    .orElseThrow(() -> new BusinessException("Boutique introuvable", HttpStatus.NOT_FOUND));
            StoreSettings s = storeSettingsRepository.findByFournisseurId(f.getId())
                    .orElseThrow(() -> new BusinessException("Paramètres introuvables", HttpStatus.NOT_FOUND));
            return new ResolvedStore(f, s);
        }
        throw new BusinessException("Slug boutique requis", HttpStatus.BAD_REQUEST);
    }

    private void mergeTestCredentials(StoreSettings settings, PaymentCredentialsTestRequest request) {
        if (request == null) return;
        if (notBlank(request.getStripePublishableKey())) {
            settings.setStripePublishableKey(request.getStripePublishableKey().trim());
        }
        if (notBlank(request.getStripeSecretKey())) {
            settings.setStripeSecretKey(request.getStripeSecretKey().trim());
        }
        if (notBlank(request.getPaypalClientId())) {
            settings.setPaypalClientId(request.getPaypalClientId().trim());
        }
        if (notBlank(request.getPaypalClientSecret())) {
            settings.setPaypalClientSecret(request.getPaypalClientSecret().trim());
        }
        if (notBlank(request.getPaypalMode())) {
            settings.setPaypalMode(FournisseurService.normalizePaypalMode(request.getPaypalMode()));
        }
        if (notBlank(request.getCmiClientId())) {
            settings.setCmiClientId(request.getCmiClientId().trim());
        }
        if (notBlank(request.getCmiStoreKey())) {
            settings.setCmiStoreKey(request.getCmiStoreKey().trim());
        }
    }

    private String fetchPayPalAccessToken(String clientId, String clientSecret, String mode) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBasicAuth(clientId, clientSecret);
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "client_credentials");
        ResponseEntity<String> response = restTemplate.exchange(
                paypalBaseUrl(mode) + "/v1/oauth2/token",
                HttpMethod.POST,
                new HttpEntity<>(form, headers),
                String.class);
        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            throw new BusinessException("Authentification PayPal échouée", HttpStatus.BAD_REQUEST);
        }
        try {
            String token = objectMapper.readTree(response.getBody()).path("access_token").asText();
            if (!notBlank(token)) {
                throw new BusinessException("Token PayPal manquant", HttpStatus.BAD_REQUEST);
            }
            return token;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException("Réponse auth PayPal invalide", HttpStatus.BAD_REQUEST);
        }
    }

    private static String paypalBaseUrl(String mode) {
        return "live".equalsIgnoreCase(mode)
                ? "https://api-m.paypal.com"
                : "https://api-m.sandbox.paypal.com";
    }

    private record ResolvedStore(Fournisseur fournisseur, StoreSettings settings) {}

    private static boolean notBlank(String v) {
        return v != null && !v.isBlank();
    }

    private static boolean blank(String v) {
        return v == null || v.isBlank();
    }

    private static String firstNonBlank(String a, String b) {
        if (notBlank(a)) return a;
        if (notBlank(b)) return b;
        return null;
    }
}
