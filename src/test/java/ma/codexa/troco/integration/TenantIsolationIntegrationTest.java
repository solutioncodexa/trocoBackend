package ma.codexa.troco.integration;

import com.fasterxml.jackson.databind.JsonNode;
import ma.codexa.troco.support.IntegrationTestBase;
import ma.codexa.troco.tenant.TenantResolutionFilter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Intégration — isolation multi-tenant")
class TenantIsolationIntegrationTest extends IntegrationTestBase {

    @Test
    @DisplayName("Produits d'un tenant invisibles pour l'autre (header + query)")
    void productsIsolatedBetweenTenants() throws Exception {
        String slugA = uniqueSlug("shop-a");
        String slugB = uniqueSlug("shop-b");
        String emailA = uniqueEmail("a");
        String emailB = uniqueEmail("b");

        registerStore(newStoreRequest("Shop A", slugA, emailA, "Password123!"));
        registerStore(newStoreRequest("Shop B", slugB, emailB, "Password123!"));

        String tokenA = login(emailA, "Password123!");
        String tokenB = login(emailB, "Password123!");

        createCategory(tokenA, "Cat A", "cat-a");
        createCategory(tokenB, "Cat B", "cat-b");

        String productIdA = createProduct(tokenA, "cat-a", "Produit Exclusive A");
        createProduct(tokenB, "cat-b", "Produit Exclusive B");

        JsonNode productsA = readData(mockMvc.perform(get("/products")
                        .header(TenantResolutionFilter.HEADER_SLUG, slugA))
                .andExpect(status().isOk())
                .andReturn())
                .path("content");

        assertThat(productsA.isArray()).isTrue();
        assertThat(productsA.toString()).contains("Produit Exclusive A");
        assertThat(productsA.toString()).doesNotContain("Produit Exclusive B");

        JsonNode productsB = readData(mockMvc.perform(get("/products")
                        .header(TenantResolutionFilter.HEADER_SLUG, slugB))
                .andExpect(status().isOk())
                .andReturn())
                .path("content");
        assertThat(productsB.toString()).contains("Produit Exclusive B");
        assertThat(productsB.toString()).doesNotContain("Produit Exclusive A");

        JsonNode productsBViaQuery = readData(mockMvc.perform(get("/products").param("tenant", slugB))
                .andExpect(status().isOk())
                .andReturn())
                .path("content");
        assertThat(productsBViaQuery.toString()).contains("Produit Exclusive B");
        assertThat(productsBViaQuery.toString()).doesNotContain("Produit Exclusive A");

        // Admin B ne voit pas le produit A même par ID (filtre tenant)
        mockMvc.perform(get("/products/" + productIdA)
                        .header("Authorization", "Bearer " + tokenB)
                        .header(TenantResolutionFilter.HEADER_SLUG, slugB))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Catégories seed Troco visibles avec tenant troco")
    void trocoSeedCategoriesVisible() throws Exception {
        mockMvc.perform(get("/categories")
                        .header(TenantResolutionFilter.HEADER_SLUG, "troco"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].slug").isNotEmpty());
    }
}
