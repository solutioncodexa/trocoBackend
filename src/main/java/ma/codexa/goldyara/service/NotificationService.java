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
        });
    }

    @Transactional
    public void markAllAsRead() {
        notificationRepository.findByReadFalseOrderByCreatedAtDesc()
                .forEach(n -> {
                    n.setRead(true);
                    notificationRepository.save(n);
                });
    }
}
