package ma.codexa.troco.integration;

import com.fasterxml.jackson.databind.JsonNode;
import ma.codexa.troco.dto.request.CreateFournisseurRequest;
import ma.codexa.troco.support.IntegrationTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Intégration — plateforme Matjarona")
class PlatformIntegrationTest extends IntegrationTestBase {

    @Test
    @DisplayName("GET /platform/plans retourne au moins le plan Basic/Starter")
    void listPlans_returnsBasic() throws Exception {
        mockMvc.perform(get("/platform/plans"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[?(@.code=='basic')].priceMad").value(org.hamcrest.Matchers.hasItem(150.0)));
    }

    @Test
    @DisplayName("GET /platform/themes liste les designs vitrine")
    void listThemes() throws Exception {
        mockMvc.perform(get("/platform/themes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].key").exists());
    }

    @Test
    @DisplayName("Inscription publique → PENDING ; activation Super Admin → store public OK")
    void registerStore_pending_thenActivate() throws Exception {
        String slug = uniqueSlug("boutique");
        String email = uniqueEmail("vendeur");
        CreateFournisseurRequest req = newStoreRequest("Boutique Test", slug, email, "Password123!");

        JsonNode created = registerStore(req);
        assertThat(created.path("slug").asText()).isEqualTo(slug);
        assertThat(created.path("name").asText()).isEqualTo("Boutique Test");
        assertThat(created.path("status").asText()).isEqualTo("PENDING");
        long id = created.path("id").asLong();

        mockMvc.perform(get("/platform/store").param("slug", slug))
                .andExpect(status().isForbidden());

        String token = login(email, "Password123!");
        assertThat(token).isNotBlank();

        String saToken = login("superadmin@matjarona.ma", "SuperAdmin1234");
        mockMvc.perform(patch("/platform/fournisseurs/" + id + "/status")
                        .header("Authorization", "Bearer " + saToken)
                        .contentType("application/json")
                        .content(json(Map.of("status", "ACTIVE"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));

        mockMvc.perform(get("/platform/store").param("slug", slug))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.siteName").value("Boutique Test"))
                .andExpect(jsonPath("$.data.slug").value(slug))
                .andExpect(jsonPath("$.data.themeKey").value("classic"));
    }

    @Test
    @DisplayName("Slug déjà pris → 409")
    void registerStore_duplicateSlug_conflict() throws Exception {
        String slug = uniqueSlug("dup");
        registerStore(newStoreRequest("First", slug, uniqueEmail("a"), "Password123!"));

        postJson("/platform/register", newStoreRequest("Second", slug, uniqueEmail("b"), "Password123!"))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("Super Admin liste les fournisseurs ; ADMIN boutique est refusé")
    void superAdmin_listFournisseurs() throws Exception {
        String slug = uniqueSlug("sa");
        String vendorEmail = uniqueEmail("vendor");
        registerStore(newStoreRequest("SA List", slug, vendorEmail, "Password123!"));

        String vendorToken = login(vendorEmail, "Password123!");
        mockMvc.perform(get("/platform/fournisseurs")
                        .header("Authorization", "Bearer " + vendorToken))
                .andExpect(status().isForbidden());

        String saToken = login("superadmin@matjarona.ma", "SuperAdmin1234");
        mockMvc.perform(get("/platform/fournisseurs")
                        .header("Authorization", "Bearer " + saToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @DisplayName("Super Admin peut suspendre une boutique")
    void superAdmin_updateStatus() throws Exception {
        String slug = uniqueSlug("suspend");
        JsonNode created = registerStore(newStoreRequest("To Suspend", slug, uniqueEmail("sus"), "Password123!"));
        long id = created.path("id").asLong();

        String saToken = login("superadmin@matjarona.ma", "SuperAdmin1234");
        // Activer d'abord (inscription = PENDING)
        mockMvc.perform(patch("/platform/fournisseurs/" + id + "/status")
                        .header("Authorization", "Bearer " + saToken)
                        .contentType("application/json")
                        .content(json(Map.of("status", "ACTIVE"))))
                .andExpect(status().isOk());

        mockMvc.perform(patch("/platform/fournisseurs/" + id + "/status")
                        .header("Authorization", "Bearer " + saToken)
                        .contentType("application/json")
                        .content(json(Map.of("status", "SUSPENDED"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("SUSPENDED"));
    }
}
