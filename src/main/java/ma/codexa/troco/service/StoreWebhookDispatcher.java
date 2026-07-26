package ma.codexa.troco.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ma.codexa.troco.entity.StoreWebhook;
import ma.codexa.troco.entity.StoreWebhookDelivery;
import ma.codexa.troco.repository.StoreWebhookDeliveryRepository;
import ma.codexa.troco.repository.StoreWebhookRepository;
import ma.codexa.troco.tenant.TenantContext;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class StoreWebhookDispatcher {

    public static final String EVENT_ORDER_CREATED = "order.created";
    public static final String EVENT_LEAD_CREATED = "lead.created";

    private final StoreWebhookRepository webhookRepository;
    private final StoreWebhookDeliveryRepository deliveryRepository;
    /** Boot 4 expose JsonMapper (Jackson 3), pas ObjectMapper — instance locale. */
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(8))
            .build();

    @Async
    public void dispatchAsync(String eventType, Map<String, Object> payload) {
        Long fid = TenantContext.getFournisseurId();
        if (fid == null) return;
        dispatchAsync(fid, eventType, payload);
    }

    @Async
    public void dispatchAsync(Long fournisseurId, String eventType, Map<String, Object> payload) {
        if (fournisseurId == null) return;
        try {
            dispatch(fournisseurId, eventType, payload);
        } catch (Exception e) {
            log.warn("webhook_dispatch_failed event={} err={}", eventType, e.getMessage());
        }
    }

    @Transactional
    public void dispatch(Long fournisseurId, String eventType, Map<String, Object> payload) {
        TenantContext.setBypass(false);
        TenantContext.setFournisseurId(fournisseurId);
        try {
            List<StoreWebhook> hooks = webhookRepository.findByEnabledTrue();
            for (StoreWebhook hook : hooks) {
                if (!listens(hook.getEvents(), eventType)) continue;
                deliver(hook, eventType, payload);
            }
        } finally {
            TenantContext.clear();
        }
    }

    private void deliver(StoreWebhook hook, String eventType, Map<String, Object> payload) {
        StoreWebhookDelivery delivery = new StoreWebhookDelivery();
        delivery.setFournisseurId(hook.getFournisseurId());
        delivery.setWebhookId(hook.getId());
        delivery.setEventType(eventType);

        try {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("event", eventType);
            body.put("timestamp", System.currentTimeMillis());
            body.put("data", payload != null ? payload : Map.of());
            String json = objectMapper.writeValueAsString(body);

            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(hook.getTargetUrl().trim()))
                    .timeout(Duration.ofSeconds(12))
                    .header("Content-Type", "application/json")
                    .header("User-Agent", "Matjarona-Webhooks/1.0")
                    .header("X-Matjarona-Event", eventType)
                    .POST(HttpRequest.BodyPublishers.ofString(json));

            if (hook.getSecret() != null && !hook.getSecret().isBlank()) {
                builder.header("X-Matjarona-Signature", "sha256=" + hmacSha256(hook.getSecret(), json));
            }

            HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            int code = response.statusCode();
            delivery.setStatusCode(code);
            delivery.setSuccess(code >= 200 && code < 300);
            if (!delivery.getSuccess()) {
                String err = response.body();
                if (err != null && err.length() > 480) err = err.substring(0, 480);
                delivery.setErrorMessage(err);
            }
        } catch (Exception e) {
            delivery.setSuccess(false);
            delivery.setErrorMessage(e.getMessage() != null && e.getMessage().length() > 480
                    ? e.getMessage().substring(0, 480) : e.getMessage());
        }
        deliveryRepository.save(delivery);
        log.info("webhook_delivery webhookId={} event={} success={} status={}",
                hook.getId(), eventType, delivery.getSuccess(), delivery.getStatusCode());
    }

    private static boolean listens(String eventsCsv, String eventType) {
        if (eventsCsv == null || eventType == null) return false;
        for (String part : eventsCsv.split(",")) {
            if (eventType.equalsIgnoreCase(part.trim())) return true;
        }
        return false;
    }

    private static String hmacSha256(String secret, String body) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(body.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            return "";
        }
    }

    public static boolean isAllowedEvent(String event) {
        if (event == null) return false;
        String e = event.trim().toLowerCase(Locale.ROOT);
        return EVENT_ORDER_CREATED.equals(e) || EVENT_LEAD_CREATED.equals(e);
    }
}
