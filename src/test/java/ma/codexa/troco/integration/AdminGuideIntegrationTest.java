package ma.codexa.troco.integration;

import com.fasterxml.jackson.databind.JsonNode;
import ma.codexa.troco.support.IntegrationTestBase;
import ma.codexa.troco.tenant.TenantResolutionFilter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Intégration — guide 1ère utilisation + recherche produit")
class AdminGuideIntegrationTest extends IntegrationTestBase {

    @Test
    @DisplayName("GET /auth/me expose adminGuideCompleted=false puis PATCH le marque terminé")
    void adminGuidePersistedPerUser() throws Exception {
        SeededStore store = seedStore("guide");

        mockMvc.perform(get("/auth/me")
                        .header("Authorization", "Bearer " + store.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.adminGuideCompleted").value(false));

        JsonNode after = readData(authPatch("/auth/me/admin-guide", store.token(), store.slug(),
                Map.of("completed", true))
                .andExpect(status().isOk())
                .andReturn());
        assertThat(after.path("adminGuideCompleted").asBoolean()).isTrue();

        mockMvc.perform(get("/auth/me")
                        .header("Authorization", "Bearer " + store.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.adminGuideCompleted").value(true));

        // Réouverture du guide (completed=false)
        readData(authPatch("/auth/me/admin-guide", store.token(), store.slug(),
                Map.of("completed", false))
                .andExpect(status().isOk())
                .andReturn());
        mockMvc.perform(get("/auth/me")
                        .header("Authorization", "Bearer " + store.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.adminGuideCompleted").value(false));
    }

    @Test
    @DisplayName("Dashboard + stock overview OK sans stock_settings précréés (pas d'INSERT read-only)")
    void dashboardAndStockOverviewWithoutSettingsRow() throws Exception {
        SeededStore store = seedStore("dash-stock");

        mockMvc.perform(get("/stats/dashboard")
                        .header("Authorization", "Bearer " + store.token())
                        .header(TenantResolutionFilter.HEADER_SLUG, store.slug()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.productCount").exists());

        mockMvc.perform(get("/stock/overview")
                        .header("Authorization", "Bearer " + store.token())
                        .header(TenantResolutionFilter.HEADER_SLUG, store.slug()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("Recherche produits par keyword (LOWER description) ne renvoie pas 500")
    void productKeywordSearch() throws Exception {
        SeededStore store = seedStore("kw");
        createCategory(store.token(), "KW Cat", "kw-cat");
        createProduct(store.token(), "kw-cat", "Cameran Sykes Unique KW");

        mockMvc.perform(get("/products")
                        .param("page", "0")
                        .param("size", "12")
                        .param("keyword", "Cameran")
                        .header("Authorization", "Bearer " + store.token())
                        .header(TenantResolutionFilter.HEADER_SLUG, store.slug()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}
