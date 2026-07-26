package ma.codexa.troco.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ma.codexa.troco.dto.AbandonedCartDTO;
import ma.codexa.troco.dto.request.CaptureAbandonedCartRequest;
import ma.codexa.troco.entity.AbandonedCart;
import ma.codexa.troco.entity.StoreSettings;
import ma.codexa.troco.repository.AbandonedCartRepository;
import ma.codexa.troco.repository.StoreSettingsRepository;
import ma.codexa.troco.tenant.TenantContext;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AbandonedCartService {

    private final AbandonedCartRepository repository;
    private final StoreSettingsRepository storeSettingsRepository;
    private final EmailService emailService;
    private final NotificationService notificationService;
    /** Boot 4 expose JsonMapper (Jackson 3), pas ObjectMapper — instance locale. */
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Transactional
    public AbandonedCartDTO capture(CaptureAbandonedCartRequest req) {
        Long fid = TenantContext.getFournisseurId();
        if (fid == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Boutique non résolue");
        }
        StoreSettings settings = storeSettingsRepository.findByFournisseurId(fid).orElse(null);
        if (settings != null && !settings.isAbandonedCartEnabled()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Relance panier désactivée");
        }
        if ((req.getCustomerEmail() == null || req.getCustomerEmail().isBlank())
                && (req.getCustomerPhone() == null || req.getCustomerPhone().isBlank())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email ou téléphone requis");
        }
        if (req.getItems() == null || req.getItems().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Panier vide");
        }

        int delay = settings != null && settings.getAbandonedCartDelayMinutes() != null
                ? settings.getAbandonedCartDelayMinutes() : 60;

        AbandonedCart cart = repository.findBySessionKey(req.getSessionKey().trim())
                .orElseGet(() -> {
                    AbandonedCart c = new AbandonedCart();
                    c.setFournisseurId(fid);
                    c.setSessionKey(req.getSessionKey().trim());
                    c.setRecoveryToken(UUID.randomUUID().toString().replace("-", ""));
                    return c;
                });

        cart.setCustomerEmail(blank(req.getCustomerEmail()));
        cart.setCustomerPhone(blank(req.getCustomerPhone()));
        cart.setCustomerName(blank(req.getCustomerName()));
        cart.setCartJson(writeJson(req.getItems()));
        cart.setCartTotal(req.getCartTotal() != null ? req.getCartTotal() : BigDecimal.ZERO);
        cart.setItemCount(req.getItems().size());
        cart.setLastActivityAt(LocalDateTime.now());
        cart.setRemindAt(LocalDateTime.now().plusMinutes(delay));
        cart.setReminderSent(false);
        cart.setRecovered(false);
        return toDto(repository.save(cart));
    }

    @Transactional(readOnly = true)
    public AbandonedCartDTO recover(String token) {
        AbandonedCart cart = repository.findByRecoveryToken(token)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lien invalide"));
        return toDto(cart);
    }

    @Transactional
    public void markRecovered(String sessionKey) {
        if (sessionKey == null || sessionKey.isBlank()) return;
        repository.findBySessionKey(sessionKey.trim()).ifPresent(c -> {
            c.setRecovered(true);
            repository.save(c);
        });
    }

    @Transactional(readOnly = true)
    public List<AbandonedCartDTO> listAdmin() {
        return repository.findAllByOrderByUpdatedAtDesc().stream().map(this::toDto).collect(Collectors.toList());
    }

    @Transactional
    public int processDueReminders() {
        TenantContext.setBypass(true);
        int sent = 0;
        try {
            for (AbandonedCart cart : repository.findDueReminders(LocalDateTime.now())) {
                Long fid = cart.getFournisseurId();
                if (fid == null) continue;
                TenantContext.setFournisseurId(fid);
                try {
                    sendReminder(cart);
                    cart.setReminderSent(true);
                    repository.save(cart);
                    sent++;
                } catch (Exception e) {
                    log.warn("abandoned_cart_reminder_failed id={}: {}", cart.getId(), e.getMessage());
                } finally {
                    TenantContext.setFournisseurId(null);
                }
            }
        } finally {
            TenantContext.clear();
        }
        return sent;
    }

    private void sendReminder(AbandonedCart cart) {
        String site = storeSettingsRepository.findByFournisseurId(cart.getFournisseurId())
                .map(StoreSettings::getSiteName).orElse("votre boutique");
        String recoveryPath = "/panier?recover=" + cart.getRecoveryToken();
        if (cart.getCustomerEmail() != null && !cart.getCustomerEmail().isBlank()) {
            emailService.sendAbandonedCartReminder(
                    cart.getCustomerEmail(),
                    cart.getCustomerName() != null ? cart.getCustomerName() : "Bonjour",
                    site,
                    cart.getCartTotal() != null ? cart.getCartTotal().doubleValue() : 0,
                    recoveryPath // le client ouvre ce chemin sur le domaine de la boutique
            );
        }
        notificationService.notifyAbandonedCart(
                cart.getId(),
                cart.getCustomerName(),
                cart.getCustomerPhone(),
                cart.getCustomerEmail(),
                cart.getCartTotal()
        );
    }

    private AbandonedCartDTO toDto(AbandonedCart c) {
        return new AbandonedCartDTO(
                c.getId(), c.getSessionKey(), c.getRecoveryToken(),
                c.getCustomerEmail(), c.getCustomerPhone(), c.getCustomerName(),
                c.getCartJson(), c.getCartTotal(),
                c.getItemCount() != null ? c.getItemCount() : 0,
                Boolean.TRUE.equals(c.getReminderSent()),
                Boolean.TRUE.equals(c.getRecovered()),
                c.getRemindAt(), c.getLastActivityAt(), c.getCreatedAt()
        );
    }

    private String writeJson(Object o) {
        try {
            return objectMapper.writeValueAsString(o);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Panier invalide");
        }
    }

    private static String blank(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
