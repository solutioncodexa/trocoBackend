package ma.codexa.goldyara.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private static final String FROM = "GoldYara <noreply.codexa@gmail.com>";
    private static final String SUBJECT_ORDER = "[GoldYara] Nouvelle commande reçue";
    private static final String SUBJECT_CUSTOM_ORDER = "[GoldYara] Nouvelle demande personnalisée";

    private final JavaMailSender mailSender;

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
                "Montant total : %.2f MAD\n\n" +
                "Connectez-vous à l'interface admin pour plus de détails.",
                customerName, orderNumber, totalAmount != null ? totalAmount : 0.0
        );
        sendToAdmins(adminEmails, SUBJECT_ORDER, text);
    }

    @Async
    public void sendNewCustomOrderNotification(List<String> adminEmails, String customerName, Long customOrderId) {
        if (adminEmails == null || adminEmails.isEmpty()) {
            log.warn("No admin emails to send custom order notification");
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

    private void sendToAdmins(List<String> emails, String subject, String text) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom("noreply.codexa@gmail.com");
            message.setTo(emails.toArray(new String[0]));
            message.setSubject(subject);
            message.setText(text);
            mailSender.send(message);
            log.info("Order notification email sent to {} admins", emails.size());
        } catch (Exception e) {
            log.error("Failed to send notification email: {}", e.getMessage());
        }
    }
}
