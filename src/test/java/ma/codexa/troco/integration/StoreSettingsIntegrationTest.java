package ma.codexa.troco.integration;

import com.fasterxml.jackson.databind.JsonNode;
import ma.codexa.troco.dto.request.CreateFournisseurRequest;
import ma.codexa.troco.dto.request.UpdateStoreSettingsRequest;
import ma.codexa.troco.support.IntegrationTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Intégration — paramètres boutique")
class StoreSettingsIntegrationTest extends IntegrationTestBase {

    @Test
    @DisplayName("Admin lit et met à jour /store-settings/me")
    void updateMyStoreSettings() throws Exception {
        String slug = uniqueSlug("settings");
        String email = uniqueEmail("settings");
        CreateFournisseurRequest req = newStoreRequest("Settings Shop", slug, email, "Password123!");
        JsonNode created = registerStore(req);
        long fournisseurId = created.path("id").asLong();
        String token = login(email, "Password123!");
        String saToken = login("superadmin@matjarona.ma", "SuperAdmin1234");
        mockMvc.perform(patch("/platform/fournisseurs/" + fournisseurId + "/status")
                        .header("Authorization", "Bearer " + saToken)
                        .contentType("application/json")
                        .content(json(Map.of("status", "ACTIVE"))))
                .andExpect(status().isOk());

        mockMvc.perform(get("/store-settings/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.siteName").value("Settings Shop"));

        UpdateStoreSettingsRequest update = new UpdateStoreSettingsRequest();
        update.setSiteName("Settings Shop Pro");
        update.setTagline("Livraison rapide");
        update.setPrimaryColor("#C2410C");
        update.setSecondaryColor("#7C2D12");
        update.setContactPhone("0699887766");
        update.setContactWhatsapp("+212612345678");
        update.setWhatsappOrderTemplate("Bonjour, je veux {productName} ({url})");
        update.setFacebookUrl("https://facebook.com/maison-atlas-test");
        update.setInstagramUrl("instagram.com/maison.atlas.test");
        update.setTiktokUrl("https://www.tiktok.com/@maisonatlas");
        update.setFreeShippingThreshold(new BigDecimal("500"));
        update.setSurMesureEnabled(false);
        update.setHeroEnabled(true);
        update.setCategoriesEnabled(true);

        JsonNode updated = readData(mockMvc.perform(put("/store-settings/me")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(json(update)))
                .andExpect(status().isOk())
                .andReturn());

        assertThat(updated.path("siteName").asText()).isEqualTo("Settings Shop Pro");
        assertThat(updated.path("primaryColor").asText()).isEqualToIgnoringCase("#C2410C");
        assertThat(updated.path("surMesureEnabled").asBoolean()).isFalse();
        assertThat(updated.path("freeShippingThreshold").asDouble()).isEqualTo(500.0);
        assertThat(updated.path("contactWhatsapp").asText()).isEqualTo("+212612345678");
        assertThat(updated.path("whatsappOrderTemplate").asText()).contains("{productName}");

        mockMvc.perform(get("/platform/store").param("slug", slug))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.siteName").value("Settings Shop Pro"))
                .andExpect(jsonPath("$.data.tagline").value("Livraison rapide"))
                .andExpect(jsonPath("$.data.contactWhatsapp").value("+212612345678"))
                .andExpect(jsonPath("$.data.whatsappOrderTemplate").value("Bonjour, je veux {productName} ({url})"));

        // Sync automatique vers les réseaux sociaux (footer / contact / WhatsApp flottant)
        JsonNode social = readData(mockMvc.perform(get("/social-networks")
                        .header("Authorization", "Bearer " + token)
                        .header("X-Fournisseur-Slug", slug))
                .andExpect(status().isOk())
                .andReturn());
        java.util.Map<String, JsonNode> byKey = new java.util.HashMap<>();
        for (JsonNode n : social) {
            byKey.put(n.path("networkKey").asText().toLowerCase(), n);
        }
        assertThat(byKey.get("whatsapp").path("enabled").asBoolean()).isTrue();
        assertThat(byKey.get("whatsapp").path("url").asText()).contains("wa.me/212612345678");
        assertThat(byKey.get("facebook").path("enabled").asBoolean()).isTrue();
        assertThat(byKey.get("facebook").path("url").asText()).contains("facebook.com/maison-atlas-test");
        assertThat(byKey.get("instagram").path("enabled").asBoolean()).isTrue();
        assertThat(byKey.get("instagram").path("url").asText()).startsWith("https://");
        assertThat(byKey.get("tiktok").path("enabled").asBoolean()).isTrue();
        assertThat(byKey.get("tiktok").path("url").asText()).contains("tiktok.com");
    }

    @Test
    @DisplayName("verify-domain sans domaine configuré → 400")
    void verifyDomain_withoutDomain() throws Exception {
        String email = uniqueEmail("domain");
        registerStore(newStoreRequest("No Domain", uniqueSlug("nodom"), email, "Password123!"));
        String token = login(email, "Password123!");

        mockMvc.perform(post("/store-settings/me/verify-domain")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Sans auth → refus sur store-settings")
    void storeSettings_requiresAuth() throws Exception {
        mockMvc.perform(get("/store-settings/me"))
                .andExpect(result -> {
                    int code = result.getResponse().getStatus();
                    org.assertj.core.api.Assertions.assertThat(code).isIn(401, 403);
                });
    }
}
