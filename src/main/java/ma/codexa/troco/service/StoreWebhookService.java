package ma.codexa.troco.service;

import lombok.RequiredArgsConstructor;
import ma.codexa.troco.dto.StoreWebhookDTO;
import ma.codexa.troco.dto.StoreWebhookDeliveryDTO;
import ma.codexa.troco.dto.StoreWebhookListItemDTO;
import ma.codexa.troco.dto.request.UpsertStoreWebhookRequest;
import ma.codexa.troco.entity.StoreWebhook;
import ma.codexa.troco.entity.StoreWebhookDelivery;
import ma.codexa.troco.repository.StoreWebhookDeliveryRepository;
import ma.codexa.troco.repository.StoreWebhookRepository;
import ma.codexa.troco.tenant.TenantContext;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StoreWebhookService {

    private final StoreWebhookRepository webhookRepository;
    private final StoreWebhookDeliveryRepository deliveryRepository;
    private final AuditLogService auditLogService;
    private final PlanEntitlementService planEntitlementService;

    @Transactional(readOnly = true)
    public List<StoreWebhookListItemDTO> list() {
        return webhookRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::toListItem)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<StoreWebhookDeliveryDTO> recentDeliveries() {
        return deliveryRepository.findTop50ByOrderByCreatedAtDesc().stream().map(this::toDeliveryDto).collect(Collectors.toList());
    }

    @Transactional
    public StoreWebhookDTO create(UpsertStoreWebhookRequest req) {
        planEntitlementService.assertWebhooksAllowed();
        Long fid = TenantContext.requireFournisseurId();
        StoreWebhook w = new StoreWebhook();
        w.setFournisseurId(fid);
        apply(w, req, true);
        StoreWebhook saved = webhookRepository.save(w);
        auditLogService.record(AuditLogService.Action.WEBHOOK_UPDATE, "WEBHOOK",
                String.valueOf(saved.getId()), "Webhook créé: " + saved.getName());
        // Secret renvoyé une seule fois à la création.
        return toDto(saved);
    }

    @Transactional
    public StoreWebhookListItemDTO update(Long id, UpsertStoreWebhookRequest req) {
        StoreWebhook w = require(id);
        apply(w, req, false);
        StoreWebhook saved = webhookRepository.save(w);
        auditLogService.record(AuditLogService.Action.WEBHOOK_UPDATE, "WEBHOOK",
                String.valueOf(saved.getId()), "Webhook mis à jour: " + saved.getName());
        return toListItem(saved);
    }

    @Transactional
    public void delete(Long id) {
        StoreWebhook w = require(id);
        webhookRepository.delete(w);
        auditLogService.record(AuditLogService.Action.WEBHOOK_UPDATE, "WEBHOOK",
                String.valueOf(id), "Webhook supprimé");
    }

    private void apply(StoreWebhook w, UpsertStoreWebhookRequest req, boolean creating) {
        String url = req.getTargetUrl().trim();
        if (!url.startsWith("https://") && !url.startsWith("http://localhost") && !url.startsWith("http://127.0.0.1")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "URL webhook doit être https (ou localhost en test)");
        }
        List<String> events = req.getEvents().stream()
                .map(e -> e == null ? "" : e.trim().toLowerCase(Locale.ROOT))
                .filter(StoreWebhookDispatcher::isAllowedEvent)
                .distinct()
                .toList();
        if (events.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Aucun événement valide (order.created, lead.created)");
        }
        for (String event : events) {
            planEntitlementService.assertWebhookEventAllowed(event);
        }
        w.setName(req.getName().trim());
        w.setTargetUrl(url);
        if (req.getSecret() != null && !req.getSecret().isBlank()) {
            w.setSecret(req.getSecret().trim());
        } else if (creating) {
            w.setSecret(null);
        }
        w.setEvents(String.join(",", events));
        if (req.getEnabled() != null) w.setEnabled(req.getEnabled());
    }

    private StoreWebhook require(Long id) {
        return webhookRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Webhook introuvable"));
    }

    private List<String> parseEvents(StoreWebhook w) {
        return w.getEvents() == null ? List.of()
                : Arrays.stream(w.getEvents().split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();
    }

    private StoreWebhookListItemDTO toListItem(StoreWebhook w) {
        return new StoreWebhookListItemDTO(
                w.getId(), w.getName(), w.getTargetUrl(),
                w.getSecret() != null && !w.getSecret().isBlank(),
                parseEvents(w),
                Boolean.TRUE.equals(w.getEnabled()), w.getCreatedAt(), w.getUpdatedAt());
    }

    private StoreWebhookDTO toDto(StoreWebhook w) {
        return new StoreWebhookDTO(
                w.getId(), w.getName(), w.getTargetUrl(), w.getSecret(), parseEvents(w),
                Boolean.TRUE.equals(w.getEnabled()), w.getCreatedAt(), w.getUpdatedAt());
    }

    private StoreWebhookDeliveryDTO toDeliveryDto(StoreWebhookDelivery d) {
        return new StoreWebhookDeliveryDTO(
                d.getId(), d.getWebhookId(), d.getEventType(), d.getStatusCode(),
                Boolean.TRUE.equals(d.getSuccess()), d.getErrorMessage(), d.getCreatedAt());
    }
}
