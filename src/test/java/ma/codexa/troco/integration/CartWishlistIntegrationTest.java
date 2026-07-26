package ma.codexa.troco.integration;

import com.fasterxml.jackson.databind.JsonNode;
import ma.codexa.troco.support.IntegrationTestBase;
import ma.codexa.troco.tenant.TenantResolutionFilter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Intégration — panier & favoris")
class CartWishlistIntegrationTest extends IntegrationTestBase {

    @Test
    @DisplayName("Panier: get → add → update qty → remove → clear")
    void cartLifecycle() throws Exception {
        SeededStore store = seedStore("cart");
        createCategory(store.token(), "Cart Cat", "cart-cat");
        String productId = createProduct(store.token(), "cart-cat", "Produit Panier Test");
        String sessionId = "sess-" + UUID.randomUUID();

        mockMvc.perform(get("/cart")
                        .param("sessionId", sessionId)
                        .header(TenantResolutionFilter.HEADER_SLUG, store.slug()))
                .andExpect(status().isOk());

        JsonNode cart = objectMapper.readTree(mockMvc.perform(post("/cart/items")
                        .param("sessionId", sessionId)
                        .header(TenantResolutionFilter.HEADER_SLUG, store.slug())
                        .contentType("application/json")
                        .content(json(Map.of("productId", Long.parseLong(productId), "quantity", 2))))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsByteArray());

        assertThat(cart.toString()).contains(productId);
        long itemId = cart.path("items").get(0).path("id").asLong();

        mockMvc.perform(put("/cart/items/" + itemId)
                        .param("sessionId", sessionId)
                        .header(TenantResolutionFilter.HEADER_SLUG, store.slug())
                        .contentType("application/json")
                        .content(json(Map.of("quantity", 5))))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/cart/items/" + itemId)
                        .param("sessionId", sessionId)
                        .header(TenantResolutionFilter.HEADER_SLUG, store.slug()))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/cart")
                        .param("sessionId", sessionId)
                        .header(TenantResolutionFilter.HEADER_SLUG, store.slug()))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("Wishlist: add → list → remove → clear")
    void wishlistLifecycle() throws Exception {
        SeededStore store = seedStore("wish");
        createCategory(store.token(), "Wish Cat", "wish-cat");
        String productId = createProduct(store.token(), "wish-cat", "Produit Favori");
        long customerId = 900_000L + Math.abs(UUID.randomUUID().getLeastSignificantBits() % 100_000);

        mockMvc.perform(post("/wishlist/" + customerId + "/products/" + productId)
                        .header(TenantResolutionFilter.HEADER_SLUG, store.slug()))
                .andExpect(status().isOk());

        mockMvc.perform(get("/wishlist/" + customerId)
                        .header(TenantResolutionFilter.HEADER_SLUG, store.slug()))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/wishlist/" + customerId + "/products/" + productId)
                        .header(TenantResolutionFilter.HEADER_SLUG, store.slug()))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/wishlist/" + customerId)
                        .header(TenantResolutionFilter.HEADER_SLUG, store.slug()))
                .andExpect(status().isNoContent());
    }
}
