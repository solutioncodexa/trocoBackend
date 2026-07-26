package ma.codexa.troco.integration;

import com.fasterxml.jackson.databind.JsonNode;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Intégration — devis / sur-mesure")
class CustomOrderIntegrationTest extends IntegrationTestBase {

    @Test
    @DisplayName("Client soumet un devis ; admin liste et change le statut")
    void submitAndManageCustomOrder() throws Exception {
        SeededStore store = seedStore("devis");

        ObjectNode body = objectMapper.createObjectNode();
        body.put("description", "Besoin de 500 sachets kraft personnalisés");
        body.put("type", "BRACELET");
        body.put("style", "MODERNE");
        body.put("size", "M");
        ObjectNode customer = body.putObject("customer");
        customer.put("fullName", "Client Devis");
        customer.put("phone", "0611223344");
        customer.put("email", uniqueEmail("devis-client"));
        customer.put("city", "Rabat");
        customer.put("address", "Avenue Hassan II");

        JsonNode created = objectMapper.readTree(mockMvc.perform(post("/custom-orders")
                        .header(TenantResolutionFilter.HEADER_SLUG, store.slug())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsBytes(body)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsByteArray());

        String id = created.path("id").asText();
        assertThat(id).isNotBlank();

        mockMvc.perform(get("/custom-orders")
                        .header("Authorization", "Bearer " + store.token())
                        .header(TenantResolutionFilter.HEADER_SLUG, store.slug()))
                .andExpect(status().isOk());

        mockMvc.perform(patch("/custom-orders/" + id + "/status")
                        .header("Authorization", "Bearer " + store.token())
                        .header(TenantResolutionFilter.HEADER_SLUG, store.slug())
                        .contentType("application/json")
                        .content(json(Map.of("status", "IN_REVIEW"))))
                .andExpect(status().isOk());
    }
}
