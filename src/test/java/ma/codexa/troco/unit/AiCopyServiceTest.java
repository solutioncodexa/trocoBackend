package ma.codexa.troco.unit;

import ma.codexa.troco.dto.request.AiCopyRequest;
import ma.codexa.troco.service.AiCopyService;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AiCopyServiceTest {

    private final AiCopyService service = new AiCopyService();

    @Test
    void generatesSeoTitle() {
        AiCopyRequest req = new AiCopyRequest();
        req.setKind("seo_title");
        req.setTopic("Sacs kraft");
        req.setStoreName("Atlas");
        req.setTone("friendly");
        Map<String, Object> out = service.generate(req);
        assertThat(out.get("text").toString()).contains("Sacs kraft");
        assertThat(out.get("text").toString().length()).isLessThanOrEqualTo(60);
    }

    @Test
    void generatesFaqItems() {
        AiCopyRequest req = new AiCopyRequest();
        req.setKind("faq");
        req.setTopic("emballage");
        req.setStoreName("Atlas");
        @SuppressWarnings("unchecked")
        List<Map<String, String>> items = (List<Map<String, String>>) service.generate(req).get("items");
        assertThat(items).isNotEmpty();
        assertThat(items.get(0)).containsKeys("q", "a");
    }

    @Test
    void rejectsUnknownKind() {
        AiCopyRequest req = new AiCopyRequest();
        req.setKind("unknown");
        req.setTopic("x");
        assertThatThrownBy(() -> service.generate(req))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("inconnu");
    }
}
