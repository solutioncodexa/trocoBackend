package ma.codexa.troco.integration;

import com.fasterxml.jackson.databind.JsonNode;
import ma.codexa.troco.support.IntegrationTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Intégration — cycle de vie tokens auth")
class AuthTokenLifecycleIntegrationTest extends IntegrationTestBase {

    @Test
    @DisplayName("Login → refresh → logout invalide le refresh")
    void loginRefreshLogout() throws Exception {
        SeededStore store = seedStore("auth-life");

        JsonNode loginData = readData(postJson("/auth/login", Map.of(
                        "email", store.email(),
                        "password", store.password()))
                .andExpect(status().isOk())
                .andReturn());

        String access = loginData.path("access_token").asText();
        String refresh = loginData.path("refresh_token").asText();
        assertThat(access).isNotBlank();
        assertThat(refresh).isNotBlank();

        mockMvc.perform(get("/auth/me").header("Authorization", "Bearer " + access))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value(store.email()));

        JsonNode refreshed = readData(postJson("/auth/refresh", Map.of("refreshToken", refresh))
                .andExpect(status().isOk())
                .andReturn());
        assertThat(refreshed.path("access_token").asText()).isNotBlank();

        String newRefresh = refreshed.path("refresh_token").asText(refresh);
        postJson("/auth/logout", Map.of("refreshToken", newRefresh))
                .andExpect(status().isOk());

        postJson("/auth/refresh", Map.of("refreshToken", newRefresh))
                .andExpect(result -> {
                    int code = result.getResponse().getStatus();
                    org.assertj.core.api.Assertions.assertThat(code).isIn(401, 403, 400);
                });
    }

    @Test
    @DisplayName("Register refuse un email déjà pris")
    void registerDuplicateEmail() throws Exception {
        SeededStore store = seedStore("dup-email");
        postJson("/auth/register", Map.of(
                        "email", store.email(),
                        "password", "Customer12!"))
                .andExpect(result -> {
                    int code = result.getResponse().getStatus();
                    org.assertj.core.api.Assertions.assertThat(code).isIn(400, 409);
                });
    }
}
