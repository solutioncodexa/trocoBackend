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

@DisplayName("Intégration — paniers abandonnés")
class AbandonedCartIntegrationTest extends IntegrationTestBase {

    @Test
    @DisplayName("capture → recover → admin list → mark recovered")
    void captureRecoverList() throws Exception {
        SeededStore store = seedStore("abandoned");

        MvcResult captured = mockMvc.perform(post("/abandoned-carts/public/capture")
                        .header(TenantResolutionFilter.HEADER_SLUG, store.slug())
                        .contentType("application/json")
                        .content(json(Map.of(
                                "sessionKey", "sess-" + store.slug(),
                                "customerEmail", "client+" + store.slug() + "@test.local",
                                "customerPhone", "+212600000099",
                                "customerName", "Client Relance",
                                "cartTotal", 49.5,
                                "items", List.of(Map.of(
                                        "productId", "1",
                                        "quantity", 2,
                                        "name", "Produit Test",
                                        "price", 24.75
                                ))))))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode cart = readData(captured);
        String token = cart.path("recoveryToken").asText();
        assertThat(token).isNotBlank();
        assertThat(cart.path("itemCount").asInt()).isEqualTo(1);
        assertThat(cart.path("recovered").asBoolean()).isFalse();

        mockMvc.perform(get("/abandoned-carts/public/recover/" + token)
                        .header(TenantResolutionFilter.HEADER_SLUG, store.slug()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.recoveryToken").value(token))
                .andExpect(jsonPath("$.data.customerName").value("Client Relance"));

        JsonNode adminList = readData(mockMvc.perform(get("/abandoned-carts")
                        .header("Authorization", "Bearer " + store.token())
                        .header(TenantResolutionFilter.HEADER_SLUG, store.slug()))
                .andExpect(status().isOk())
                .andReturn());
        assertThat(adminList.isArray()).isTrue();
        assertThat(adminList).isNotEmpty();
        assertThat(adminList.get(0).path("customerEmail").asText()).contains("@test.local");

        mockMvc.perform(post("/abandoned-carts/public/recovered")
                        .header(TenantResolutionFilter.HEADER_SLUG, store.slug())
                        .contentType("application/json")
                        .content(json(Map.of("sessionKey", "sess-" + store.slug()))))
                .andExpect(status().isOk());

        JsonNode after = readData(mockMvc.perform(get("/abandoned-carts/public/recover/" + token)
                        .header(TenantResolutionFilter.HEADER_SLUG, store.slug()))
                .andExpect(status().isOk())
                .andReturn());
        assertThat(after.path("recovered").asBoolean()).isTrue();
    }
}
