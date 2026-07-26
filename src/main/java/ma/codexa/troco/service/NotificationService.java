package ma.codexa.troco.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ma.codexa.troco.entity.Notification;
import ma.codexa.troco.entity.Order;
import ma.codexa.troco.entity.CustomOrder;
import ma.codexa.troco.entity.User;
import ma.codexa.troco.repository.NotificationRepository;
import ma.codexa.troco.repository.UserRepository;
import ma.codexa.troco.tenant.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;

    @Transactional
    public void notifyNewOrder(Order order) {
        Notification notification = new Notification();
        notification.setType("ORDER");
        notification.setTitle("Nouvelle commande");
        notification.setMessage(String.format("Commande #%s - %s - %.2f DH",
                order.getOrderNumber(),
                order.getCustomer() != null ? order.getCustomer().getFullName() : "N/A",
                order.getTotalAmount() != null ? order.getTotalAmount() : 0.0));
        notification.setReferenceId(order.getId());
        notificationRepository.save(notification);

        List<String> adminEmails = userRepository.findByRole("ADMIN").stream()
                .map(User::getEmail)
                .toList();
        log.info("notification_recorded kind=ORDER orderId={} orderNumber={} adminRecipients={}",
                order.getId(), order.getOrderNumber(), adminEmails.size());
        if (adminEmails.isEmpty()) {
            log.warn("notification_recorded_no_admin_emails kind=ORDER orderId={}", order.getId());
        }
        emailService.sendNewOrderNotification(
                adminEmails,
                order.getCustomer() != null ? order.getCustomer().getFullName() : "N/A",
                order.getOrderNumber(),
                order.getTotalAmount()
        );
    }

    /**
     * Alerte stock (LOW_STOCK / OUT_OF_STOCK / EXPIRING_SOON).
     * Anti-spam : une seule notification non lue par type + variante.
     */
    @Transactional
    public void notifyStockAlert(String type, String title, String message, Long variantId) {
        if (variantId != null && notificationRepository.existsByTypeAndReferenceIdAndReadFalse(type, variantId)) {
            log.debug("stock_alert_skipped_duplicate type={} variantId={}", type, variantId);
            return;
        }
        Notification notification = new Notification();
        notification.setType(type);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setReferenceId(variantId);
        notificationRepository.save(notification);
        log.info("notification_recorded kind={} variantId={}", type, variantId);
    }

    @Transactional
    public void notifyAbandonedCart(
            Long cartId,
            String customerName,
            String customerPhone,
            String customerEmail,
            java.math.BigDecimal cartTotal) {
        Notification notification = new Notification();
        notification.setType("ABANDONED_CART");
        notification.setTitle("Panier abandonné");
        notification.setMessage(String.format(
                "%s — %s / %s — %.2f DH",
                customerName != null ? customerName : "Client",
                customerPhone != null ? customerPhone : "-",
                customerEmail != null ? customerEmail : "-",
                cartTotal != null ? cartTotal.doubleValue() : 0));
        notification.setReferenceId(cartId);
        notification.setFournisseurId(TenantContext.getFournisseurId());
        notificationRepository.save(notification);
        log.info("notification_recorded kind=ABANDONED_CART cartId={}", cartId);
    }

    @Transactional
    public void notifyPageScheduleChange(Long pageId, String title, String slug, boolean nowLive) {
        Notification notification = new Notification();
        notification.setType(nowLive ? "PAGE_LIVE" : "PAGE_OFFLINE");
        notification.setTitle(nowLive ? "Page en ligne" : "Page hors ligne");
        notification.setMessage(String.format(
                nowLive
                        ? "La page « %s » (%s) est maintenant visible sur la vitrine."
                        : "La page « %s » (%s) n’est plus visible sur la vitrine.",
                title != null ? title : "Page",
                slug != null ? slug : "-"));
        notification.setReferenceId(pageId);
        notification.setFournisseurId(TenantContext.getFournisseurId());
        notificationRepository.save(notification);
        log.info("notification_recorded kind={} pageId={} live={}",
                notification.getType(), pageId, nowLive);
    }

    @Transactional
    public void notifyNewCustomOrder(CustomOrder customOrder) {
        Notification notification = new Notification();
        notification.setType("CUSTOM_ORDER");
        notification.setTitle("Nouvelle demande personnalisée");
        notification.setMessage(String.format("Demande #%d - %s",
                customOrder.getId(),
                customOrder.getCustomer() != null ? customOrder.getCustomer().getFullName() : "N/A"));
        notification.setReferenceId(customOrder.getId());
        notificationRepository.save(notification);

        List<String> adminEmails = userRepository.findByRole("ADMIN").stream()
                .map(User::getEmail)
                .toList();
        log.info("notification_recorded kind=CUSTOM_ORDER customOrderId={} adminRecipients={}",
                customOrder.getId(), adminEmails.size());
        if (adminEmails.isEmpty()) {
            log.warn("notification_recorded_no_admin_emails kind=CUSTOM_ORDER customOrderId={}", customOrder.getId());
        }
        emailService.sendNewCustomOrderNotification(
                adminEmails,
                customOrder.getCustomer() != null ? customOrder.getCustomer().getFullName() : "N/A",
                customOrder.getId()
        );
    }

    @Transactional(readOnly = true)
    public List<Notification> getAllNotifications() {
        return notificationRepository.findAllByOrderByCreatedAtDesc();
    }

    @Transactional(readOnly = true)
    public List<Notification> getUnreadNotifications() {
        return notificationRepository.findByReadFalseOrderByCreatedAtDesc();
    }

    @Transactional(readOnly = true)
    public long getUnreadCount() {
        return notificationRepository.countByReadFalse();
    }

    @Transactional
    public void markAsRead(Long id) {
        notificationRepository.findById(id).ifPresent(n -> {
            n.setRead(true);
            notificationRepository.save(n);
            log.info("notification_mark_read notificationId={}", id);
        });
    }

    @Transactional
    public void markAllAsRead() {
        var unread = notificationRepository.findByReadFalseOrderByCreatedAtDesc();
        unread.forEach(n -> {
            n.setRead(true);
            notificationRepository.save(n);
        });
        log.info("notification_mark_all_read count={}", unread.size());
    }
}
