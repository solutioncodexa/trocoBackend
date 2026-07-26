package ma.codexa.troco.integration;

import ma.codexa.troco.support.IntegrationTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Intégration — authentification")
class AuthIntegrationTest extends IntegrationTestBase {

    @Test
    @DisplayName("Login admin Troco seed + /auth/me")
    void loginAdmin_andMe() throws Exception {
        String token = login("admin@troco.ma", "Admin1234");

        mockMvc.perform(get("/auth/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value("admin@troco.ma"))
                .andExpect(jsonPath("$.data.role").value("ADMIN"));
    }

    @Test
    @DisplayName("Login Super Admin sans fournisseurId")
    void loginSuperAdmin() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType("application/json")
                        .content(json(Map.of(
                                "email", "superadmin@matjarona.ma",
                                "password", "SuperAdmin1234"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.role").value("SUPER_ADMIN"))
                .andExpect(jsonPath("$.data.fournisseur_id").doesNotExist());
    }

    @Test
    @DisplayName("Mauvais mot de passe → refus")
    void login_badPassword() throws Exception {
        postJson("/auth/login", Map.of("email", "admin@troco.ma", "password", "wrong-password"))
                .andExpect(result -> {
                    int code = result.getResponse().getStatus();
                    org.assertj.core.api.Assertions.assertThat(code).isIn(401, 403);
                });
    }

    @Test
    @DisplayName("Inscription client CUSTOMER")
    void registerCustomer() throws Exception {
        String email = uniqueEmail("client");
        postJson("/auth/register", Map.of(
                        "email", email,
                        "password", "Customer12!"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.role").value("CUSTOMER"))
                .andExpect(jsonPath("$.data.access_token").isNotEmpty());
    }

    @Test
    @DisplayName("/auth/me sans token → refus")
    void me_unauthenticated() throws Exception {
        mockMvc.perform(get("/auth/me"))
                .andExpect(result -> {
                    int code = result.getResponse().getStatus();
                    org.assertj.core.api.Assertions.assertThat(code).isIn(401, 403);
                });
    }
}
