package ma.codexa.goldyara.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ma.codexa.goldyara.entity.Notification;
import ma.codexa.goldyara.entity.Order;
import ma.codexa.goldyara.entity.CustomOrder;
import ma.codexa.goldyara.entity.User;
import ma.codexa.goldyara.repository.NotificationRepository;
import ma.codexa.goldyara.repository.UserRepository;
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
        notification.setMessage(String.format("Commande #%s - %s - %.2f MAD",
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
