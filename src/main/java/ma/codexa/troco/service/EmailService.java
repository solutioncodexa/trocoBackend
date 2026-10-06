package ma.codexa.troco.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import jakarta.mail.internet.MimeMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private static final String SUBJECT_ORDER = "[Troco] Nouvelle commande reçue";
    private static final String SUBJECT_CUSTOM_ORDER = "[Troco] Nouvelle demande personnalisée";
    private static final String PLATFORM_BRAND = "Get STORE";

    private final JavaMailSender mailSender;

    @Value("${app.mail.sender-name:Get STORE}")
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
        sendHtmlToMany(adminEmails, SUBJECT_ORDER, text);
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
        String display = platformDisplayName();
        if (mailUsername == null || mailUsername.isBlank()) {
            log.warn("spring.mail.username vide — From incorrect pour SMTP");
            return display + " <noreply@localhost>";
        }
        return String.format("%s <%s>", display, mailUsername.trim());
    }

    /** Nom d'expéditeur plateforme ; les anciennes valeurs (Matjarona/Troco) sont remplacées par Get STORE. */
    private String platformDisplayName() {
        String name = senderDisplayName != null ? senderDisplayName.trim() : "";
        String lower = name.toLowerCase();
        if (name.isEmpty() || lower.contains("matjarona") || lower.equals("troco")) return PLATFORM_BRAND;
        return name;
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
            sendHtml(customerEmail.trim(),
                    "Votre panier vous attend — " + (storeName != null ? storeName : "Boutique"),
                    text);
            log.info("abandoned_cart_email_sent to={}", customerEmail);
        } catch (Exception e) {
            log.error("Failed to send abandoned cart email: {}", e.getMessage());
        }
    }

    @Async
    public void sendTrialEndingSoon(String to, String storeName, long daysLeft) {
        sendHtml(to, "Votre essai gratuit Get STORE se termine bientôt",
                String.format("Bonjour,\n\nL'essai gratuit de votre boutique « %s » se termine dans %d jour(s).\n\n"
                        + "Pour garder votre boutique en ligne, contactez-nous afin de choisir votre plan.\n\n"
                        + "L'équipe Get STORE", storeName, daysLeft));
    }

    @Async
    public void sendTrialEnded(String to, String storeName) {
        sendHtml(to, "Votre essai gratuit Get STORE est terminé",
                String.format("Bonjour,\n\nL'essai gratuit de votre boutique « %s » est terminé : la vitrine est "
                        + "temporairement fermée. Vos données sont conservées.\n\n"
                        + "Connectez-vous à votre espace admin ou contactez-nous pour choisir votre plan et réactiver la boutique.\n\n"
                        + "L'équipe Get STORE", storeName));
    }

    @Async
    public void sendEmailVerification(String to, String fullName, String storeName, String link) {
        if (to == null || to.isBlank()) return;
        boolean hasName = fullName != null && !fullName.isBlank();
        boolean hasShop = storeName != null && !storeName.isBlank();
        String hello = hasName ? "Bonjour " + fullName.trim() + "," : "Bonjour,";
        String shop = hasShop ? " de votre boutique « " + storeName.trim() + " »" : "";
        String subject = "Confirmez votre adresse email — " + PLATFORM_BRAND;

        String text = hello + "\n\nPour sécuriser le compte" + shop + " et recevoir nos notifications, "
                + "confirmez votre adresse email (lien valable 48 h) :\n\n"
                + link + "\n\nSi vous n'êtes pas à l'origine de cette inscription, ignorez ce message.\n\n"
                + "L'équipe " + PLATFORM_BRAND;

        String htmlShop = hasShop ? " de votre boutique <strong>« " + escapeHtml(storeName.trim()) + " »</strong>" : "";
        String safeLink = escapeHtml(link);
        String html = """
                <!DOCTYPE html>
                <html lang="fr">
                <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>%1$s</title>
                </head>
                <body style="margin:0;padding:0;background-color:#f4f5f7;font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,Helvetica,Arial,sans-serif;color:#1f2937;">
                <span style="display:none;max-height:0;overflow:hidden;opacity:0;">Confirmez votre adresse email pour activer votre compte %2$s.</span>
                <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" border="0" style="background-color:#f4f5f7;">
                  <tr>
                    <td align="center" style="padding:32px 16px;">
                      <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" border="0" style="max-width:560px;">
                        <tr>
                          <td align="center" style="padding-bottom:24px;">
                            <span style="font-size:24px;font-weight:800;letter-spacing:-0.5px;color:#111827;">Get <span style="color:#4f46e5;">STORE</span></span>
                          </td>
                        </tr>
                        <tr>
                          <td style="background-color:#ffffff;border-radius:16px;padding:40px 32px;box-shadow:0 1px 3px rgba(0,0,0,0.06);">
                            <h1 style="margin:0 0 16px;font-size:22px;line-height:1.3;font-weight:700;color:#111827;">Confirmez votre adresse email</h1>
                            <p style="margin:0 0 16px;font-size:16px;line-height:1.6;">%3$s</p>
                            <p style="margin:0 0 28px;font-size:16px;line-height:1.6;">Pour sécuriser le compte%4$s et recevoir nos notifications, merci de confirmer votre adresse email.</p>
                            <table role="presentation" cellspacing="0" cellpadding="0" border="0" align="center" style="margin:0 auto 28px;">
                              <tr>
                                <td align="center" style="border-radius:10px;background-color:#4f46e5;">
                                  <a href="%5$s" target="_blank" style="display:inline-block;padding:14px 32px;font-size:16px;font-weight:600;color:#ffffff;text-decoration:none;border-radius:10px;">Vérifier mon compte</a>
                                </td>
                              </tr>
                            </table>
                            <p style="margin:0 0 8px;font-size:14px;line-height:1.6;color:#6b7280;">Ce bouton est valable <strong>48 heures</strong>.</p>
                            <p style="margin:0;font-size:14px;line-height:1.6;color:#6b7280;">Si vous n'êtes pas à l'origine de cette inscription, vous pouvez ignorer ce message en toute sécurité.</p>
                          </td>
                        </tr>
                        <tr>
                          <td align="center" style="padding:24px 16px 0;font-size:12px;line-height:1.6;color:#9ca3af;">
                            Le bouton ne fonctionne pas ? Copiez ce lien dans votre navigateur :<br>
                            <a href="%5$s" style="color:#6b7280;word-break:break-all;">%5$s</a>
                            <br><br>L'équipe %2$s
                          </td>
                        </tr>
                      </table>
                    </td>
                  </tr>
                </table>
                </body>
                </html>
                """.formatted(escapeHtml(subject), PLATFORM_BRAND, escapeHtml(hello), htmlShop, safeLink);

        sendHtml(to, subject, text, html);
    }

    private void sendHtml(String to, String subject, String text) {
        if (to == null || to.isBlank()) return;
        sendHtml(to, subject, text, layout(subject, paragraphs(text)));
    }

    private void sendHtml(String to, String subject, String text, String html) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());
            helper.setFrom(fromHeader());
            helper.setTo(to.trim());
            helper.setSubject(subject);
            helper.setText(text, html);
            mailSender.send(message);
            log.info("email_sent subject={} to={}", subject, to);
        } catch (Exception e) {
            log.error("Failed to send email '{}': {}", subject, e.getMessage());
        }
    }

    private static String escapeHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }

    private static String paragraphs(String text) {
        String[] blocks = (text == null ? "" : text).split("\\n\\n");
        StringBuilder sb = new StringBuilder();
        for (String block : blocks) {
            if (block.isBlank()) continue;
            sb.append("<p style=\"margin:0 0 16px;font-size:16px;line-height:1.6;\">")
                    .append(escapeHtml(block).replace("\n", "<br>"))
                    .append("</p>");
        }
        return sb.toString();
    }

    private static String layout(String title, String innerHtml) {
        return """
                <!DOCTYPE html>
                <html lang="fr">
                <head><meta charset="UTF-8"><meta name="viewport" content="width=device-width, initial-scale=1.0"><title>%1$s</title></head>
                <body style="margin:0;padding:0;background-color:#f4f5f7;font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,Helvetica,Arial,sans-serif;color:#1f2937;">
                <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" border="0" style="background-color:#f4f5f7;">
                  <tr><td align="center" style="padding:32px 16px;">
                    <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" border="0" style="max-width:560px;">
                      <tr><td align="center" style="padding-bottom:24px;">
                        <span style="font-size:24px;font-weight:800;letter-spacing:-0.5px;color:#111827;">Get <span style="color:#4f46e5;">STORE</span></span>
                      </td></tr>
                      <tr><td style="background-color:#ffffff;border-radius:16px;padding:40px 32px;">
                        <h1 style="margin:0 0 16px;font-size:22px;line-height:1.3;font-weight:700;color:#111827;">%1$s</h1>
                        %2$s
                      </td></tr>
                      <tr><td align="center" style="padding:24px 16px 0;font-size:12px;line-height:1.6;color:#9ca3af;">L'équipe Get STORE</td></tr>
                    </table>
                  </td></tr>
                </table>
                </body></html>
                """.formatted(escapeHtml(title), innerHtml);
    }

    /**
     * Email transactionnel à un client de boutique : expéditeur au nom de la boutique
     * (adresse SMTP de la plateforme), réponses dirigées vers l'email de contact de la boutique.
     */
    @Async
    public void sendCustomerMail(String to, String subject, String text, String storeName, String replyTo) {
        if (to == null || to.isBlank()) return;
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());
            String display = storeName != null && !storeName.isBlank() ? storeName.trim() : senderDisplayName.trim();
            String address = mailUsername != null && !mailUsername.isBlank() ? mailUsername.trim() : "noreply@localhost";
            helper.setFrom(String.format("%s <%s>", display, address));
            if (replyTo != null && !replyTo.isBlank()) helper.setReplyTo(replyTo.trim());
            helper.setTo(to.trim());
            helper.setSubject(subject);
            helper.setText(text, layout(subject, paragraphs(text)));
            mailSender.send(message);
            log.info("customer_email_sent subject={} to={}", subject, to);
        } catch (Exception e) {
            log.error("Failed to send customer email '{}': {}", subject, e.getMessage());
        }
    }

    private void sendHtmlToMany(List<String> emails, String subject, String text) {
        if (emails == null || emails.isEmpty()) return;
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());
            helper.setFrom(fromHeader());
            helper.setTo(emails.toArray(new String[0]));
            helper.setSubject(subject);
            helper.setText(text, layout(subject, paragraphs(text)));
            mailSender.send(message);
            log.info("notification_email_sent adminCount={} fromUser={}", emails.size(), mailUsername);
        } catch (Exception e) {
            log.error("Failed to send notification email: {}", e.getMessage());
        }
    }

    private void sendPlain(String to, String subject, String text) {
        if (to == null || to.isBlank()) return;
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromHeader());
            message.setTo(to.trim());
            message.setSubject(subject);
            message.setText(text);
            mailSender.send(message);
            log.info("email_sent subject={} to={}", subject, to);
        } catch (Exception e) {
            log.error("Failed to send email '{}': {}", subject, e.getMessage());
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
