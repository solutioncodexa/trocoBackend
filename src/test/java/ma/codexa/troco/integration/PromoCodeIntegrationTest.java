package ma.codexa.troco.integration;

import com.fasterxml.jackson.databind.JsonNode;
import ma.codexa.troco.dto.CreatePromoCodeRequest;
import ma.codexa.troco.support.IntegrationTestBase;
import ma.codexa.troco.tenant.TenantResolutionFilter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Intégration — codes promo")
class PromoCodeIntegrationTest extends IntegrationTestBase {

    @Test
    @DisplayName("Admin crée un code ; public le valide / suggestions")
    void createValidateTogglePromo() throws Exception {
        SeededStore store = seedStore("promo");

        CreatePromoCodeRequest req = new CreatePromoCodeRequest();
        req.setCode("ATLAS15");
        req.setType("reusable");
        req.setDiscountType("percentage");
        req.setDiscountValue(15.0);
        req.setMinOrderAmount(100.0);
        req.setMaxUses(50);
        req.setIsActive(true);

        JsonNode created = readData(authPost("/promo-codes", store.token(), store.slug(), req)
                .andExpect(status().isCreated())
                .andReturn());
        long id = created.path("id").asLong();
        assertThat(created.path("code").asText()).isEqualToIgnoringCase("ATLAS15");

        mockMvc.perform(get("/promo-codes/validate")
                        .param("code", "ATLAS15")
                        .param("orderTotal", "200")
                        .header(TenantResolutionFilter.HEADER_SLUG, store.slug()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        mockMvc.perform(get("/promo-codes/suggestions")
                        .param("orderTotal", "200")
                        .header(TenantResolutionFilter.HEADER_SLUG, store.slug()))
                .andExpect(status().isOk());

        mockMvc.perform(get("/promo-codes/public")
                        .header(TenantResolutionFilter.HEADER_SLUG, store.slug()))
                .andExpect(status().isOk());

        mockMvc.perform(patch("/promo-codes/" + id + "/toggle")
                        .param("isActive", "false")
                        .header("Authorization", "Bearer " + store.token())
                        .header(TenantResolutionFilter.HEADER_SLUG, store.slug()))
                .andExpect(status().isOk());
    }
}
