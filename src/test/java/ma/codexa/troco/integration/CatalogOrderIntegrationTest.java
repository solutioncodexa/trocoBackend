package ma.codexa.troco.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import ma.codexa.troco.support.IntegrationTestBase;
import ma.codexa.troco.tenant.TenantResolutionFilter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Intégration — catalogue, commande, promo")
class CatalogOrderIntegrationTest extends IntegrationTestBase {

    @Test
    @DisplayName("Création catégorie + produit + commande COD sous le bon tenant")
    void createProductAndOrder() throws Exception {
        String slug = uniqueSlug("order");
        String email = uniqueEmail("order-admin");
        registerStore(newStoreRequest("Order Shop", slug, email, "Password123!"));
        String token = login(email, "Password123!");

        createCategory(token, "Emballage", "emballage");
        String productId = createProduct(token, "emballage", "Sachet Kraft Test");

        ObjectNode order = objectMapper.createObjectNode();
        ArrayNode items = order.putArray("items");
        ObjectNode item = items.addObject();
        ObjectNode product = item.putObject("product");
        product.put("id", productId);
        product.put("price", 99.0);
        item.put("quantity", 2);
        ObjectNode customer = order.putObject("customer");
        customer.put("fullName", "Client COD");
        customer.put("phone", "0611223344");
        customer.put("address", "12 Rue Test");
        customer.put("city", "Casablanca");
        customer.put("email", uniqueEmail("buyer"));
        order.put("paymentMethod", "cash_on_delivery");
        order.put("total", 198.0);

        JsonNode createdOrder = readData(mockMvc.perform(post("/orders")
                        .header(TenantResolutionFilter.HEADER_SLUG, slug)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsBytes(order)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn());

        assertThat(createdOrder.path("status").asText()).isNotBlank();
        assertThat(createdOrder.path("orderNumber").asText()).isNotBlank();
        // OrderCreatedDTO est allégé (pas de customer) — vérifier le client via GET détail
        mockMvc.perform(get("/orders/" + createdOrder.path("id").asText())
                        .header("Authorization", "Bearer " + token)
                        .header(TenantResolutionFilter.HEADER_SLUG, slug))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.customer.fullName").value("Client COD"));

        mockMvc.perform(get("/orders")
                        .header("Authorization", "Bearer " + token)
                        .header(TenantResolutionFilter.HEADER_SLUG, slug))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray());
    }

    @Test
    @DisplayName("Code promo TROCO10 validable pour tenant troco")
    void validatePromoCode_troco() throws Exception {
        mockMvc.perform(get("/promo-codes/validate")
                        .param("code", "TROCO10")
                        .param("orderTotal", "400")
                        .header(TenantResolutionFilter.HEADER_SLUG, "troco"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("Endpoints publics top-bar / home-hero pour troco")
    void publicContentEndpoints() throws Exception {
        mockMvc.perform(get("/top-bar-messages/public")
                        .header(TenantResolutionFilter.HEADER_SLUG, "troco"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        mockMvc.perform(get("/home-hero/public")
                        .header(TenantResolutionFilter.HEADER_SLUG, "troco"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/social-networks/public")
                        .header(TenantResolutionFilter.HEADER_SLUG, "troco"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Admin Troco peut lister les catégories seed")
    void adminListsCategories() throws Exception {
        String token = login("admin@troco.ma", "Admin1234");
        mockMvc.perform(get("/categories")
                        .header("Authorization", "Bearer " + token)
                        .header(TenantResolutionFilter.HEADER_SLUG, "troco"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(org.hamcrest.Matchers.greaterThan(0)));
    }

    @Test
    @DisplayName("Recherche produits publique")
    void searchProducts() throws Exception {
        String slug = uniqueSlug("search");
        String email = uniqueEmail("search");
        registerStore(newStoreRequest("Search Shop", slug, email, "Password123!"));
        String token = login(email, "Password123!");
        createCategory(token, "Search Cat", "search-cat");
        createProduct(token, "search-cat", "Boite Valise Premium");

        mockMvc.perform(get("/products")
                        .param("keyword", "Valise")
                        .header(TenantResolutionFilter.HEADER_SLUG, slug))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].name").value(org.hamcrest.Matchers.containsString("Valise")));

        mockMvc.perform(get("/products/search")
                        .param("keyword", "Valise")
                        .header(TenantResolutionFilter.HEADER_SLUG, slug))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}
