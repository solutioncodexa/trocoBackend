package ma.codexa.troco.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ma.codexa.troco.entity.Order;
import ma.codexa.troco.entity.OrderItem;
import ma.codexa.troco.entity.StoreSettings;
import ma.codexa.troco.repository.StoreSettingsRepository;
import org.springframework.stereotype.Service;

import java.util.Locale;

/**
 * Emails transactionnels envoyés aux clients de la boutique (confirmation, suivi, statut).
 * Best-effort : une erreur d'envoi ne doit jamais faire échouer la commande.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerNotificationService {

    private final EmailService emailService;
    private final StoreSettingsRepository storeSettingsRepository;

    /** Accusé de réception juste après la création de la commande. */
    public void orderPlaced(Order order) {
        send(order, "Commande " + order.getOrderNumber() + " bien reçue", store -> {
            StringBuilder sb = new StringBuilder();
            sb.append("Bonjour ").append(customerName(order)).append(",\n\n");
            sb.append("Merci pour votre commande chez ").append(store.name()).append(" ! ");
            sb.append("Nous l'avons bien reçue et nous la préparons.\n\n");
            sb.append("Commande n° ").append(order.getOrderNumber()).append('\n');
            if (order.getOrderItems() != null) {
                for (OrderItem item : order.getOrderItems()) {
                    sb.append("  - ")
                            .append(item.getProduct() != null ? item.getProduct().getName() : "Article")
                            .append(" × ").append(item.getQuantity() != null ? item.getQuantity() : 1)
                            .append('\n');
                }
            }
            sb.append(String.format(Locale.FRANCE, "Total : %.2f %s%n", safe(order.getTotalAmount()), store.currency()));
            sb.append("Paiement : ").append(paymentLabel(order.getPaymentMethod())).append("\n\n");
            sb.append("Nous vous préviendrons dès l'expédition de votre colis.");
            return sb.toString();
        });
    }

    /** Colis expédié (numéro de suivi renseigné). */
    public void orderShipped(Order order) {
        if (order.getTrackingNumber() == null || order.getTrackingNumber().isBlank()) return;
        send(order, "Votre commande " + order.getOrderNumber() + " est en route", store -> {
            StringBuilder sb = new StringBuilder();
            sb.append("Bonjour ").append(customerName(order)).append(",\n\n");
            sb.append("Votre commande n° ").append(order.getOrderNumber()).append(" a été expédiée.\n");
            sb.append("Numéro de suivi : ").append(order.getTrackingNumber()).append('\n');
            if (order.getTrackingUrl() != null && !order.getTrackingUrl().isBlank()) {
                sb.append("Suivre mon colis : ").append(order.getTrackingUrl()).append('\n');
            }
            return sb.toString();
        });
    }

    /** Changement de statut notable (confirmée, livrée, annulée). */
    public void orderStatusChanged(Order order, String newStatus) {
        if (newStatus == null) return;
        String status = newStatus.toUpperCase(Locale.ROOT);
        String subject;
        String line;
        switch (status) {
            case "CONFIRMED" -> {
                subject = "Commande " + order.getOrderNumber() + " confirmée";
                line = "Votre commande est confirmée et en cours de préparation.";
            }
            case "DELIVERED" -> {
                subject = "Commande " + order.getOrderNumber() + " livrée";
                line = "Votre commande a été livrée. Merci de votre confiance !";
            }
            case "CANCELLED" -> {
                subject = "Commande " + order.getOrderNumber() + " annulée";
                line = "Votre commande a été annulée. Si vous n'êtes pas à l'origine de cette annulation, contactez-nous.";
            }
            default -> {
                return;
            }
        }
        send(order, subject, store -> "Bonjour " + customerName(order) + ",\n\n" + line
                + "\n\nCommande n° " + order.getOrderNumber());
    }

    private void send(Order order, String subject, java.util.function.Function<StoreInfo, String> body) {
        try {
            String to = order.getCustomer() != null ? order.getCustomer().getEmail() : null;
            if (to == null || to.isBlank()) return;
            StoreInfo store = storeInfo(order.getFournisseurId());
            String note = store.note() != null ? "\n\n" + fill(store.note(), order, store) : "";
            String signature = store.signature() != null
                    ? "\n\n" + fill(store.signature(), order, store)
                    : "\n\n— " + store.name()
                    + (store.contactPhone() != null ? "\nTél. : " + store.contactPhone() : "")
                    + (store.contactEmail() != null ? "\nEmail : " + store.contactEmail() : "");
            emailService.sendCustomerMail(to, subject, body.apply(store) + note + signature, store.name(), store.contactEmail());
        } catch (Exception e) {
            log.warn("customer_email_failed orderId={} subject={} error={}", order.getId(), subject, e.getMessage());
        }
    }

    private StoreInfo storeInfo(Long fournisseurId) {
        StoreSettings s = fournisseurId != null
                ? storeSettingsRepository.findByFournisseurId(fournisseurId).orElse(null)
                : null;
        String name = s != null && s.getSiteName() != null && !s.getSiteName().isBlank() ? s.getSiteName().trim() : "Notre boutique";
        String currency = s != null && s.getCurrency() != null ? s.getCurrency() : "MAD";
        return new StoreInfo(name, currency, blankToNull(s != null ? s.getContactEmail() : null),
                blankToNull(s != null ? s.getContactPhone() : null),
                blankToNull(s != null ? s.getCustomerEmailNote() : null),
                blankToNull(s != null ? s.getCustomerEmailSignature() : null));
    }

    private static String customerName(Order order) {
        String n = order.getCustomer() != null ? order.getCustomer().getFullName() : null;
        return n != null && !n.isBlank() ? n.trim() : "";
    }

    private static double safe(Double v) {
        return v != null ? v : 0.0;
    }

    private static String blankToNull(String v) {
        return v == null || v.isBlank() ? null : v.trim();
    }

    private static String paymentLabel(String method) {
        if (method == null) return "paiement à la livraison";
        return switch (method.toLowerCase(Locale.ROOT)) {
            case "cash_on_delivery" -> "paiement à la livraison";
            case "card_cmi", "online" -> "paiement en ligne";
            case "bnpl" -> "paiement en plusieurs fois";
            default -> method;
        };
    }

    /** Variables disponibles dans le message et la signature du marchand. */
    static String fill(String template, Order order, StoreInfo store) {
        return template
                .replace("{client}", customerName(order))
                .replace("{boutique}", store.name())
                .replace("{commande}", order.getOrderNumber() != null ? order.getOrderNumber() : "");
    }

    record StoreInfo(String name, String currency, String contactEmail, String contactPhone, String note, String signature) {}
}
