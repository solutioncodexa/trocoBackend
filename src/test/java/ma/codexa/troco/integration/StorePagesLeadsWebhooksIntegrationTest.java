package ma.codexa.troco.integration;

import com.fasterxml.jackson.databind.JsonNode;
import ma.codexa.troco.support.IntegrationTestBase;
import ma.codexa.troco.tenant.TenantResolutionFilter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("Intégration — pages builder, leads, blog, webhooks, SEO")
class StorePagesLeadsWebhooksIntegrationTest extends IntegrationTestBase {

    @Test
    @DisplayName("Pages: créer → publier → public by-slug + preview")
    void storePageLifecycle() throws Exception {
        SeededStore store = seedStore("pages");

        MvcResult created = authPost("/store-pages", store.token(), store.slug(), Map.of(
                "title", "Notre histoire",
                "slug", "notre-histoire",
                "showInNav", true,
                "published", true,
                "isHome", false
        )).andExpect(status().isOk()).andReturn();

        long pageId = readData(created).path("id").asLong();
        assertThat(pageId).isPositive();

        mockMvc.perform(get("/store-pages/public/by-slug/notre-histoire")
                        .header(TenantResolutionFilter.HEADER_SLUG, store.slug()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.slug").value("notre-histoire"))
                .andExpect(jsonPath("$.data.currentlyLive").value(true));

        MvcResult preview = mockMvc.perform(post("/store-pages/" + pageId + "/preview-link")
                        .header("Authorization", "Bearer " + store.token())
                        .header(TenantResolutionFilter.HEADER_SLUG, store.slug()))
                .andExpect(status().isOk())
                .andReturn();
        String token = readData(preview).path("token").asText();
        assertThat(token).isNotBlank();

        mockMvc.perform(get("/store-pages/public/preview/" + token)
                        .header(TenantResolutionFilter.HEADER_SLUG, store.slug()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(pageId));
    }

    @Test
    @DisplayName("A/B home: deux variantes → promote winner")
    void abHomePromote() throws Exception {
        SeededStore store = seedStore("abhome");

        MvcResult a = authPost("/store-pages", store.token(), store.slug(), Map.of(
                "title", "Home A",
                "slug", "home-a",
                "isHome", true,
                "published", true,
                "abVariant", "A"
        )).andExpect(status().isOk()).andReturn();
        long idA = readData(a).path("id").asLong();

        authPost("/store-pages", store.token(), store.slug(), Map.of(
                "title", "Home B",
                "slug", "home-b",
                "isHome", true,
                "published", true,
                "abVariant", "B"
        )).andExpect(status().isOk());

        mockMvc.perform(get("/store-pages/public/homes")
                        .header(TenantResolutionFilter.HEADER_SLUG, store.slug()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(org.hamcrest.Matchers.greaterThanOrEqualTo(2)));

        mockMvc.perform(post("/store-pages/" + idA + "/promote-ab")
                        .header("Authorization", "Bearer " + store.token())
                        .header(TenantResolutionFilter.HEADER_SLUG, store.slug()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.isHome").value(true))
                .andExpect(jsonPath("$.data.abVariant").value(org.hamcrest.Matchers.nullValue()));
    }

    @Test
    @DisplayName("Leads public + export CSV admin")
    void storeLeads() throws Exception {
        SeededStore store = seedStore("leads");

        mockMvc.perform(post("/store-leads/public")
                        .header(TenantResolutionFilter.HEADER_SLUG, store.slug())
                        .contentType("application/json")
                        .content(json(Map.of(
                                "leadType", "newsletter",
                                "email", "client@test.local",
                                "fullName", "Sara",
                                "sourcePath", "/"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.leadType").value("newsletter"));

        mockMvc.perform(get("/store-leads")
                        .header("Authorization", "Bearer " + store.token())
                        .header(TenantResolutionFilter.HEADER_SLUG, store.slug()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)));

        mockMvc.perform(get("/store-leads/export.csv")
                        .header("Authorization", "Bearer " + store.token())
                        .header(TenantResolutionFilter.HEADER_SLUG, store.slug()))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("leads.csv")));
    }

    @Test
    @DisplayName("Blog: create → publish → public")
    void storeBlog() throws Exception {
        SeededStore store = seedStore("blog");

        MvcResult created = authPost("/store-blog", store.token(), store.slug(), Map.of(
                "title", "Guide emballage",
                "slug", "guide-emballage",
                "excerpt", "Astuces",
                "content", "Contenu long pour SEO.",
                "lang", "fr",
                "published", true
        )).andExpect(status().isOk()).andReturn();

        assertThat(readData(created).path("slug").asText()).isEqualTo("guide-emballage");

        mockMvc.perform(get("/store-blog/public")
                        .header(TenantResolutionFilter.HEADER_SLUG, store.slug()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].slug").value("guide-emballage"));

        mockMvc.perform(get("/store-blog/public/guide-emballage")
                        .header(TenantResolutionFilter.HEADER_SLUG, store.slug()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("Guide emballage"));
    }

    @Test
    @DisplayName("Webhooks CRUD + global sections + AI copy")
    void webhooksSectionsAi() throws Exception {
        SeededStore store = seedStore("hooks");

        authPost("/store-webhooks", store.token(), store.slug(), Map.of(
                "name", "Zapier commandes",
                "targetUrl", "https://hooks.zapier.com/hooks/catch/test/abc",
                "events", List.of("order.created", "lead.created"),
                "enabled", true
        )).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Zapier commandes"));

        mockMvc.perform(get("/store-webhooks")
                        .header("Authorization", "Bearer " + store.token())
                        .header(TenantResolutionFilter.HEADER_SLUG, store.slug()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1));

        mockMvc.perform(get("/store-global-sections")
                        .header("Authorization", "Bearer " + store.token())
                        .header(TenantResolutionFilter.HEADER_SLUG, store.slug()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(org.hamcrest.Matchers.greaterThanOrEqualTo(3)));

        authPut("/store-global-sections", store.token(), store.slug(), Map.of(
                "sectionKey", "sticky_cta",
                "enabled", true,
                "config", Map.of(
                        "text", "Livraison offerte",
                        "ctaLabel", "Boutique",
                        "ctaHref", "/boutique",
                        "dismissible", true
                )
        )).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.enabled").value(true));

        mockMvc.perform(get("/store-global-sections/public")
                        .header(TenantResolutionFilter.HEADER_SLUG, store.slug()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.sectionKey=='sticky_cta')].enabled").value(org.hamcrest.Matchers.hasItem(true)));

        authPost("/ai-copy/generate", store.token(), store.slug(), Map.of(
                "kind", "seo_title",
                "topic", "Sacs kraft",
                "storeName", "Maison Atlas",
                "tone", "friendly"
        )).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.text").isNotEmpty());
    }

    @Test
    @DisplayName("SEO sitemap.xml + robots.txt tenant")
    void seoSitemapAndRobots() throws Exception {
        SeededStore store = seedStore("seo");

        authPost("/store-pages", store.token(), store.slug(), Map.of(
                "title", "FAQ",
                "slug", "faq",
                "published", true,
                "showInNav", true
        )).andExpect(status().isOk());

        MvcResult sitemap = mockMvc.perform(get("/sitemap.xml")
                        .header(TenantResolutionFilter.HEADER_SLUG, store.slug())
                        .accept("application/xml"))
                .andExpect(status().isOk())
                .andReturn();
        String xml = asUtf8(sitemap);
        assertThat(xml).contains("<urlset").contains("/boutique").contains("/page/faq");

        MvcResult robots = mockMvc.perform(get("/robots.txt")
                        .header(TenantResolutionFilter.HEADER_SLUG, store.slug())
                        .accept("text/plain"))
                .andExpect(status().isOk())
                .andReturn();
        String txt = asUtf8(robots);
        assertThat(txt).contains("User-agent:").contains("Sitemap:").contains("Disallow: /admin");
    }

    @Test
    @DisplayName("Product reviews: submit → approve → public summary")
    void productReviews() throws Exception {
        SeededStore store = seedStore("reviews");
        createCategory(store.token(), "Cat Rev", "cat-rev");
        String productId = createProduct(store.token(), "cat-rev", "Produit Avis");

        mockMvc.perform(post("/product-reviews/public")
                        .header(TenantResolutionFilter.HEADER_SLUG, store.slug())
                        .contentType("application/json")
                        .content(json(Map.of(
                                "productId", Long.parseLong(productId),
                                "authorName", "Youssef",
                                "rating", 5,
                                "body", "Excellent produit, livraison rapide."))))
                .andExpect(status().isOk());

        JsonNode adminList = readData(mockMvc.perform(get("/product-reviews")
                        .header("Authorization", "Bearer " + store.token())
                        .header(TenantResolutionFilter.HEADER_SLUG, store.slug()))
                .andExpect(status().isOk())
                .andReturn());
        long reviewId = adminList.get(0).path("id").asLong();

        mockMvc.perform(patch("/product-reviews/" + reviewId + "/approve")
                        .header("Authorization", "Bearer " + store.token())
                        .header(TenantResolutionFilter.HEADER_SLUG, store.slug())
                        .contentType("application/json")
                        .content(json(Map.of("approved", true))))
                .andExpect(status().isOk());

        mockMvc.perform(get("/product-reviews/public/" + productId)
                        .header(TenantResolutionFilter.HEADER_SLUG, store.slug()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.reviewCount").value(1))
                .andExpect(jsonPath("$.data.averageRating").value(5.0));
    }
}
