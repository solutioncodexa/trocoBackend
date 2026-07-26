package ma.codexa.troco.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import ma.codexa.troco.support.IntegrationTestBase;
import ma.codexa.troco.tenant.TenantResolutionFilter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Intégration — gestion commandes admin")
class OrderManagementIntegrationTest extends IntegrationTestBase {

    @Test
    @DisplayName("Commande COD → admin liste → change statut CONFIRMED")
    void createOrderThenUpdateStatus() throws Exception {
        SeededStore store = seedStore("ordmgmt");
        createCategory(store.token(), "Ord Cat", "ord-cat");
        String productId = createProduct(store.token(), "ord-cat", "Produit Commande Admin");

        ObjectNode order = objectMapper.createObjectNode();
        ArrayNode items = order.putArray("items");
        ObjectNode item = items.addObject();
        ObjectNode product = item.putObject("product");
        product.put("id", productId);
        product.put("price", 99.0);
        item.put("quantity", 1);
        ObjectNode customer = order.putObject("customer");
        customer.put("fullName", "Client Status");
        customer.put("phone", "0600000000");
        customer.put("address", "Rue 1");
        customer.put("city", "Fès");
        customer.put("email", uniqueEmail("buyer-status"));
        order.put("paymentMethod", "cash_on_delivery");
        order.put("total", 99.0);

        JsonNode created = readData(mockMvc.perform(post("/orders")
                        .header(TenantResolutionFilter.HEADER_SLUG, store.slug())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsBytes(order)))
                .andExpect(status().isCreated())
                .andReturn());

        String orderId = created.path("id").asText();
        assertThat(orderId).isNotBlank();

        mockMvc.perform(get("/orders")
                        .header("Authorization", "Bearer " + store.token())
                        .header(TenantResolutionFilter.HEADER_SLUG, store.slug()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray());

        mockMvc.perform(patch("/orders/" + orderId + "/status")
                        .header("Authorization", "Bearer " + store.token())
                        .header(TenantResolutionFilter.HEADER_SLUG, store.slug())
                        .contentType("application/json")
                        .content(json(Map.of("status", "confirmed"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}
