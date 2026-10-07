package ma.codexa.troco.integration;

import com.fasterxml.jackson.databind.JsonNode;
import ma.codexa.troco.support.IntegrationTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Intégration — emails clients personnalisables")
class EmailTemplatesIntegrationTest extends IntegrationTestBase {

    @Test
    @DisplayName("Le marchand enregistre, relit et efface son message et sa signature")
    void saveReadClear() throws Exception {
        SeededStore store = seedStore("emailtpl");

        JsonNode saved = readData(mockMvc.perform(put("/market/email-templates")
                        .header("Authorization", "Bearer " + store.token())
                        .header("X-Fournisseur-Slug", store.slug())
                        .contentType("application/json")
                        .content(json(Map.of("note", "Merci {client} !", "signature", "L'équipe {boutique}"))))
                .andExpect(status().isOk()).andReturn());
        assertThat(saved.path("note").asText()).isEqualTo("Merci {client} !");

        JsonNode read = readData(mockMvc.perform(get("/market/email-templates")
                        .header("Authorization", "Bearer " + store.token())
                        .header("X-Fournisseur-Slug", store.slug()))
                .andExpect(status().isOk()).andReturn());
        assertThat(read.path("signature").asText()).isEqualTo("L'équipe {boutique}");

        JsonNode cleared = readData(mockMvc.perform(put("/market/email-templates")
                        .header("Authorization", "Bearer " + store.token())
                        .header("X-Fournisseur-Slug", store.slug())
                        .contentType("application/json")
                        .content(json(Map.of("note", "  ", "signature", ""))))
                .andExpect(status().isOk()).andReturn());
        assertThat(cleared.path("note").isNull()).isTrue();
        assertThat(cleared.path("signature").isNull()).isTrue();
    }

    @Test
    @DisplayName("Un message trop long est refusé, et l'endpoint exige une session admin")
    void validationAndAuth() throws Exception {
        SeededStore store = seedStore("emailtpl2");
        mockMvc.perform(put("/market/email-templates")
                        .header("Authorization", "Bearer " + store.token())
                        .header("X-Fournisseur-Slug", store.slug())
                        .contentType("application/json")
                        .content(json(Map.of("note", "x".repeat(1001)))))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/market/email-templates").header("X-Fournisseur-Slug", store.slug()))
                .andExpect(status().is4xxClientError());
    }
}
