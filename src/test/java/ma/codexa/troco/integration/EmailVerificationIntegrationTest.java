package ma.codexa.troco.integration;

import com.fasterxml.jackson.databind.JsonNode;
import ma.codexa.troco.repository.UserRepository;
import ma.codexa.troco.support.IntegrationTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Intégration — vérification d'email & réglages par défaut d'une nouvelle boutique")
class EmailVerificationIntegrationTest extends IntegrationTestBase {

    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("Inscription → compte non vérifié mais connexion OK ; le lien confirme l'email")
    void register_unverified_thenVerify() throws Exception {
        String slug = uniqueSlug("verif");
        String email = uniqueEmail("verif");
        registerStore(newStoreRequest("Boutique Verif", slug, email, "Password123!"));

        // Souple : la connexion n'est pas bloquée, /auth/me signale emailVerified=false
        String token = login(email, "Password123!");
        mockMvc.perform(get("/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.emailVerified").value(false));

        String verifyToken = userRepository.findByEmail(email).orElseThrow().getEmailVerificationToken();
        assertThat(verifyToken).isNotBlank();

        mockMvc.perform(post("/auth/verify-email")
                        .contentType("application/json")
                        .content(json(Map.of("token", verifyToken))))
                .andExpect(status().isOk());

        mockMvc.perform(get("/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.data.emailVerified").value(true));

        // Un lien ne sert qu'une fois
        mockMvc.perform(post("/auth/verify-email")
                        .contentType("application/json")
                        .content(json(Map.of("token", verifyToken))))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Lien expiré → 410 ; token inconnu → 400")
    void verify_expiredOrUnknown() throws Exception {
        String email = uniqueEmail("exp");
        registerStore(newStoreRequest("Boutique Exp", uniqueSlug("exp"), email, "Password123!"));

        var user = userRepository.findByEmail(email).orElseThrow();
        user.setEmailVerificationExpiresAt(LocalDateTime.now().minusMinutes(1));
        userRepository.save(user);

        mockMvc.perform(post("/auth/verify-email")
                        .contentType("application/json")
                        .content(json(Map.of("token", user.getEmailVerificationToken()))))
                .andExpect(status().isGone());

        mockMvc.perform(post("/auth/verify-email")
                        .contentType("application/json")
                        .content(json(Map.of("token", "inconnu"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Renvoi immédiat refusé (cooldown), non authentifié refusé")
    void resend_cooldown_andAuth() throws Exception {
        String email = uniqueEmail("resend");
        registerStore(newStoreRequest("Boutique Resend", uniqueSlug("resend"), email, "Password123!"));
        String token = login(email, "Password123!");

        mockMvc.perform(post("/auth/resend-verification").header("Authorization", "Bearer " + token))
                .andExpect(status().isTooManyRequests());
        mockMvc.perform(post("/auth/resend-verification"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Nouvelle boutique : sur-mesure désactivé par défaut")
    void newStore_surMesureDisabledByDefault() throws Exception {
        String slug = uniqueSlug("sm");
        JsonNode created = registerStore(newStoreRequest("Boutique SM", slug, uniqueEmail("sm"), "Password123!"));
        assertThat(created.path("status").asText()).isEqualTo("TRIAL");

        mockMvc.perform(get("/platform/store").param("slug", slug))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.surMesureEnabled").value(false));
    }
}
