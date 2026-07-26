package ma.codexa.troco.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private static final String SUBJECT_ORDER = "[Troco] Nouvelle commande reçue";
    private static final String SUBJECT_CUSTOM_ORDER = "[Troco] Nouvelle demande personnalisée";

    private final JavaMailSender mailSender;

    @Value("${app.mail.sender-name:Troco}")
    private String senderDisplayName;

    @Value("${spring.mail.username:}")
    private String mailUsername;

    @Async
    public void sendNewOrderNotification(List<String> adminEmails, String customerName, String orderNumber, Double totalAmount) {
        if (adminEmails == null || adminEmails.isEmpty()) {
            log.warn("No admin emails to send order notification");
            return;
        }
        String text = String.format(
                "Une nouvelle commande a été reçue.\n\n" +
                "Client : %s\n" +
                "Numéro de commande : %s\n" +
                "Montant total : %.2f DH\n\n" +
                "Connectez-vous à l'interface admin pour plus de détails.",
                customerName, orderNumber, totalAmount != null ? totalAmount : 0.0
        );
        sendToAdmins(adminEmails, SUBJECT_ORDER, text);
    }

    @Async
    public void sendNewCustomOrderNotification(List<String> adminEmails, String customerName, Long customOrderId) {
        if (adminEmails == null || adminEmails.isEmpty()) {
            log.warn("No admin emails to send notification");
            return;
        }
        String text = String.format(
                "Une nouvelle demande de commande personnalisée a été reçue.\n\n" +
                "Client : %s\n" +
                "ID : %d\n\n" +
                "Connectez-vous à l'interface admin pour traiter la demande.",
                customerName, customOrderId != null ? customOrderId : 0
        );
        sendToAdmins(adminEmails, SUBJECT_CUSTOM_ORDER, text);
    }

    /**
     * Expéditeur SMTP : même adresse que {@code spring.mail.username} (compte LWS / FAI).
     * Un From différent de l'utilisateur authentifié est souvent rejeté ou absent des dossiers « Envoyés ».
     */
    private String fromHeader() {
        if (mailUsername == null || mailUsername.isBlank()) {
            log.warn("spring.mail.username vide — From incorrect pour SMTP");
            return senderDisplayName + " <noreply@localhost>";
        }
        return String.format("%s <%s>", senderDisplayName.trim(), mailUsername.trim());
    }

    @Async
    public void sendAbandonedCartReminder(
            String customerEmail,
            String customerName,
            String storeName,
            double cartTotal,
            String recoveryPath) {
        if (customerEmail == null || customerEmail.isBlank()) return;
        String text = String.format(
                "Bonjour %s,\n\n" +
                "Vous avez laissé des articles dans votre panier chez %s (environ %.2f DH).\n\n" +
                "Finalisez votre commande ici : %s\n\n" +
                "À bientôt !",
                customerName != null ? customerName : "",
                storeName != null ? storeName : "notre boutique",
                cartTotal,
                recoveryPath
        );
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromHeader());
            message.setTo(customerEmail.trim());
            message.setSubject("Votre panier vous attend — " + (storeName != null ? storeName : "Boutique"));
            message.setText(text);
            mailSender.send(message);
            log.info("abandoned_cart_email_sent to={}", customerEmail);
        } catch (Exception e) {
            log.error("Failed to send abandoned cart email: {}", e.getMessage());
        }
    }

    private void sendToAdmins(List<String> emails, String subject, String text) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromHeader());
            message.setTo(emails.toArray(new String[0]));
            message.setSubject(subject);
            message.setText(text);
            mailSender.send(message);
            log.info("notification_email_sent adminCount={} fromUser={}", emails.size(), mailUsername);
        } catch (Exception e) {
            log.error("Failed to send notification email: {}", e.getMessage());
        }
    }
}
