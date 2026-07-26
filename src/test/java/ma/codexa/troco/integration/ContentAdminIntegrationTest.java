package ma.codexa.troco.integration;

import com.fasterxml.jackson.databind.JsonNode;
import ma.codexa.troco.support.IntegrationTestBase;
import ma.codexa.troco.tenant.TenantResolutionFilter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Intégration — contenu boutique (top-bar, social, hero, featured)")
class ContentAdminIntegrationTest extends IntegrationTestBase {

    @Test
    @DisplayName("Top-bar: admin crée ; public lit")
    void topBarPublicAndAdmin() throws Exception {
        SeededStore store = seedStore("topbar");

        mockMvc.perform(post("/top-bar-messages")
                        .header("Authorization", "Bearer " + store.token())
                        .header(TenantResolutionFilter.HEADER_SLUG, store.slug())
                        .contentType("application/json")
                        .content(json(Map.of(
                                "message", "Livraison gratuite dès 500 DH",
                                "displayOrder", 1,
                                "isActive", true))))
                .andExpect(status().isOk());

        mockMvc.perform(get("/top-bar-messages/public")
                        .header(TenantResolutionFilter.HEADER_SLUG, store.slug()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("Social networks public + admin list")
    void socialNetworks() throws Exception {
        SeededStore store = seedStore("social");
        mockMvc.perform(get("/social-networks/public")
                        .header(TenantResolutionFilter.HEADER_SLUG, store.slug()))
                .andExpect(status().isOk());

        mockMvc.perform(get("/social-networks")
                        .header("Authorization", "Bearer " + store.token())
                        .header(TenantResolutionFilter.HEADER_SLUG, store.slug()))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Home hero public")
    void homeHeroPublic() throws Exception {
        SeededStore store = seedStore("hero");
        mockMvc.perform(get("/home-hero/public")
                        .header(TenantResolutionFilter.HEADER_SLUG, store.slug()))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Featured products list publique")
    void featuredProducts() throws Exception {
        SeededStore store = seedStore("feat");
        mockMvc.perform(get("/featured-products")
                        .header(TenantResolutionFilter.HEADER_SLUG, store.slug()))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Catégories: create → update → list public")
    void categoryCrud() throws Exception {
        SeededStore store = seedStore("catcrud");
        createCategory(store.token(), "Hero Cat", "hero-cat");

        JsonNode cats = readData(mockMvc.perform(get("/categories")
                        .header(TenantResolutionFilter.HEADER_SLUG, store.slug()))
                .andExpect(status().isOk())
                .andReturn());

        long id = cats.get(0).path("id").asLong();
        mockMvc.perform(put("/categories/" + id)
                        .header("Authorization", "Bearer " + store.token())
                        .header(TenantResolutionFilter.HEADER_SLUG, store.slug())
                        .contentType("application/json")
                        .content(json(Map.of(
                                "name", "Hero Cat Updated",
                                "slug", "hero-cat",
                                "description", "Maj"))))
                .andExpect(status().isOk());
    }
}
