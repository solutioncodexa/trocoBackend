package ma.codexa.troco.support;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import ma.codexa.troco.dto.request.CreateCategoryRequest;
import ma.codexa.troco.dto.request.CreateFournisseurRequest;
import ma.codexa.troco.dto.request.CreateProductRequest;
import ma.codexa.troco.tenant.TenantResolutionFilter;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class IntegrationTestBase {

    private static final AtomicInteger SEQ = new AtomicInteger(1);

    @Autowired
    protected MockMvc mockMvc;

    /** Jackson n'expose pas toujours un bean ObjectMapper (Boot 4) — instance locale pour les tests. */
    protected final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @BeforeEach
    void ensureUploadDir() {
        new java.io.File("target/test-uploads").mkdirs();
    }

    protected String uniqueSlug(String prefix) {
        return prefix + "-" + SEQ.incrementAndGet() + "-" + UUID.randomUUID().toString().substring(0, 6);
    }

    protected String uniqueEmail(String local) {
        return local + "+" + SEQ.incrementAndGet() + "@test.matjarona.local";
    }

    protected ResultActions postJson(String path, Object body) throws Exception {
        return mockMvc.perform(post(path)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)));
    }

    protected ResultActions getJson(String path) throws Exception {
        return mockMvc.perform(get(path).accept(MediaType.APPLICATION_JSON));
    }

    protected String login(String email, String password) throws Exception {
        MvcResult result = postJson("/auth/login", Map.of("email", email, "password", password))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode data = objectMapper.readTree(result.getResponse().getContentAsString()).path("data");
        String token = data.path("access_token").asText(null);
        if (token == null || token.isBlank()) {
            throw new IllegalStateException("Login sans access_token: " + result.getResponse().getContentAsString());
        }
        return token;
    }

    protected CreateFournisseurRequest newStoreRequest(String name, String slug, String email, String password) {
        CreateFournisseurRequest req = new CreateFournisseurRequest();
        req.setName(name);
        req.setSlug(slug);
        req.setAdminEmail(email);
        req.setAdminPassword(password);
        req.setAdminFullName("Admin " + name);
        req.setPhone("0612345678");
        // Pro : maxStaff >= 2 pour les tests membres (Basic = propriétaire seul).
        req.setPlanCode("pro");
        return req;
    }

    protected JsonNode registerStore(CreateFournisseurRequest request) throws Exception {
        MvcResult result = postJson("/platform/register", request)
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).path("data");
    }

    protected void createCategory(String token, String name, String slug) throws Exception {
        CreateCategoryRequest req = new CreateCategoryRequest();
        req.setName(name);
        req.setSlug(slug);
        req.setDescription("Catégorie test");
        mockMvc.perform(post("/categories")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated());
    }

    protected String createProduct(String token, String categorySlug, String productName) throws Exception {
        CreateProductRequest req = new CreateProductRequest();
        req.setName(productName);
        req.setDescription("Description produit test intégration " + productName);
        req.setShortDescription("Court");
        req.setPrice(99.0);
        req.setCategory(categorySlug);
        req.setSku("SKU-" + UUID.randomUUID().toString().substring(0, 8));
        req.setStockQuantity(25);

        MockMultipartFile productPart = new MockMultipartFile(
                "product",
                "",
                MediaType.APPLICATION_JSON_VALUE,
                objectMapper.writeValueAsBytes(req)
        );
        // PNG 1×1 valide (évite ImageIO / optimize qui rejettent un header tronqué)
        byte[] png1x1 = new byte[]{
                (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
                0x00, 0x00, 0x00, 0x0D, 0x49, 0x48, 0x44, 0x52,
                0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x01,
                0x08, 0x02, 0x00, 0x00, 0x00, (byte) 0x90, 0x77, 0x53, (byte) 0xDE,
                0x00, 0x00, 0x00, 0x0C, 0x49, 0x44, 0x41, 0x54,
                0x08, (byte) 0xD7, 0x63, (byte) 0xF8, (byte) 0xCF, (byte) 0xC0, 0x00, 0x00,
                0x00, 0x03, 0x00, 0x01, 0x00, 0x05, (byte) 0xFE, (byte) 0xD4, (byte) 0xEF,
                0x00, 0x00, 0x00, 0x00, 0x49, 0x45, 0x4E, 0x44, (byte) 0xAE, 0x42, 0x60, (byte) 0x82
        };
        MockMultipartFile imagePart = new MockMultipartFile(
                "images",
                "test.png",
                MediaType.IMAGE_PNG_VALUE,
                png1x1
        );

        MvcResult result = mockMvc.perform(multipart("/products")
                        .file(productPart)
                        .file(imagePart)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data").path("id").asText();
    }

    protected ResultActions withTenant(ResultActions actions) {
        return actions;
    }

    protected org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder tenantGet(String path, String slug) {
        return get(path).header(TenantResolutionFilter.HEADER_SLUG, slug).accept(MediaType.APPLICATION_JSON);
    }

    protected String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }

    protected JsonNode readData(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsByteArray()).path("data");
    }

    protected String asUtf8(MvcResult result) throws Exception {
        return result.getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

    protected record SeededStore(String slug, String email, String password, String token, long fournisseurId) {}

    protected SeededStore seedStore(String namePrefix) throws Exception {
        String slug = uniqueSlug(namePrefix);
        String email = uniqueEmail(namePrefix);
        String password = "Password123!";
        JsonNode created = registerStore(newStoreRequest(namePrefix, slug, email, password));
        String token = login(email, password);
        return new SeededStore(slug, email, password, token, created.path("id").asLong());
    }

    protected JsonNode authHeadersGet(String path, String token, String tenantSlug) throws Exception {
        return readData(mockMvc.perform(get(path)
                        .header("Authorization", "Bearer " + token)
                        .header(TenantResolutionFilter.HEADER_SLUG, tenantSlug)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn());
    }

    protected ResultActions authPost(String path, String token, String tenantSlug, Object body) throws Exception {
        return mockMvc.perform(post(path)
                        .header("Authorization", "Bearer " + token)
                        .header(TenantResolutionFilter.HEADER_SLUG, tenantSlug)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)));
    }

    protected ResultActions authPut(String path, String token, String tenantSlug, Object body) throws Exception {
        return mockMvc.perform(put(path)
                        .header("Authorization", "Bearer " + token)
                        .header(TenantResolutionFilter.HEADER_SLUG, tenantSlug)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)));
    }

    protected ResultActions authPatch(String path, String token, String tenantSlug, Object body) throws Exception {
        return mockMvc.perform(patch(path)
                        .header("Authorization", "Bearer " + token)
                        .header(TenantResolutionFilter.HEADER_SLUG, tenantSlug)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)));
    }

    protected long firstVariantId(String token, String tenantSlug) throws Exception {
        JsonNode data = authHeadersGet("/stock/variants", token, tenantSlug);
        // GET /stock/variants renvoie une page { content: [...] }
        JsonNode variants = data.isArray() ? data : data.path("content");
        if (!variants.isArray() || variants.isEmpty()) {
            throw new IllegalStateException("Aucune variante stock pour le tenant " + tenantSlug);
        }
        JsonNode first = variants.get(0);
        if (first.hasNonNull("variantId")) {
            return first.path("variantId").asLong();
        }
        return first.path("id").asLong();
    }
}
