package ma.codexa.troco.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ma.codexa.troco.common.exception.BusinessException;
import ma.codexa.troco.entity.User;
import ma.codexa.troco.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HexFormat;

/**
 * Vérification d'email des comptes admin créés par inscription publique.
 * Souple : n'empêche ni la connexion ni l'essai gratuit, mais garantit une adresse joignable
 * (rappels de fin d'essai, récupération de compte).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailVerificationService {

    private static final int TOKEN_BYTES = 32;
    private static final long TOKEN_VALID_HOURS = 48;
    /** Anti-spam : un renvoi au plus toutes les 60 s (le token précédent est réutilisé s'il est encore valide). */
    private static final long RESEND_COOLDOWN_SECONDS = 60;

    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final EmailService emailService;

    @Value("${app.frontend.base-url:http://localhost:4200}")
    private String frontendBaseUrl;

    /** Marque le compte non vérifié, génère un token et envoie le lien. */
    @Transactional
    public void issueAndSend(User user, String storeName) {
        user.setEmailVerified(false);
        user.setEmailVerificationToken(newToken());
        user.setEmailVerificationExpiresAt(LocalDateTime.now().plusHours(TOKEN_VALID_HOURS));
        userRepository.save(user);
        sendLink(user, storeName);
    }

    @Transactional
    public void verify(String token) {
        if (token == null || token.isBlank()) {
            throw new BusinessException("Lien de vérification invalide", HttpStatus.BAD_REQUEST);
        }
        User user = userRepository.findByEmailVerificationToken(token.trim())
                .orElseThrow(() -> new BusinessException("Lien de vérification invalide ou déjà utilisé", HttpStatus.BAD_REQUEST));
        if (user.getEmailVerificationExpiresAt() == null
                || user.getEmailVerificationExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BusinessException("Ce lien a expiré : demandez un nouvel email de vérification", HttpStatus.GONE);
        }
        user.setEmailVerified(true);
        user.setEmailVerificationToken(null);
        user.setEmailVerificationExpiresAt(null);
        userRepository.save(user);
        log.info("email_verified userId={}", user.getId());
    }

    /** Renvoie le lien (nouveau token si absent ou expiré). No-op si déjà vérifié. */
    @Transactional
    public void resend(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException("Utilisateur non trouvé", HttpStatus.NOT_FOUND));
        if (user.isEmailVerified()) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expires = user.getEmailVerificationExpiresAt();
        boolean tokenValid = user.getEmailVerificationToken() != null && expires != null && expires.isAfter(now);
        if (tokenValid && expires.isAfter(now.plusHours(TOKEN_VALID_HOURS).minusSeconds(RESEND_COOLDOWN_SECONDS))) {
            throw new BusinessException("Un email vient d'être envoyé, patientez une minute avant de réessayer",
                    HttpStatus.TOO_MANY_REQUESTS);
        }
        if (!tokenValid) {
            user.setEmailVerificationToken(newToken());
        }
        user.setEmailVerificationExpiresAt(now.plusHours(TOKEN_VALID_HOURS));
        userRepository.save(user);
        sendLink(user, null);
    }

    private void sendLink(User user, String storeName) {
        String base = frontendBaseUrl.replaceAll("/+$", "");
        String link = base + "/verifier-email?token=" + user.getEmailVerificationToken();
        emailService.sendEmailVerification(user.getEmail(), user.getFullName(), storeName, link);
    }

    private static String newToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        RANDOM.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }
}
