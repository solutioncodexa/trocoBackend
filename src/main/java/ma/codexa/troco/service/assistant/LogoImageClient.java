package ma.codexa.troco.service.assistant;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import ma.codexa.troco.config.AssistantProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Génération d'image via l'endpoint OpenAI-compatible {@code POST {baseUrl}/images/generations}
 * (Gemini : modèles Imagen sur {@code .../v1beta/openai}). Réutilise l'URL et la clé de l'assistant.
 */
@Slf4j
@Component
public class LogoImageClient {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final int MAX_BYTES = 8 * 1024 * 1024;

    private final AssistantProperties props;
    private final String imageModel;
    private final RestClient http;
    private final int dailyLimit;
    private final AiUsageService usage;

    public LogoImageClient(AssistantProperties props, AiUsageService usage,
                           @Value("${app.assistant.image-model:imagen-3.0-generate-002}") String imageModel,
                           @Value("${app.assistant.image-daily-limit:3}") int dailyLimit) {
        this.props = props;
        this.usage = usage;
        this.imageModel = imageModel;
        this.dailyLimit = dailyLimit > 0 ? dailyLimit : 3;
        HttpClient jdk = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(jdk);
        factory.setReadTimeout(Duration.ofSeconds(Math.max(props.timeoutSeconds(), 60)));
        this.http = RestClient.builder().requestFactory(factory).build();
    }

    public int dailyLimit() {
        return dailyLimit;
    }

    /** Réserve une génération du jour pour cette boutique ; false si le quota quotidien est atteint. */
    public boolean tryReserve(Long fid) {
        return usage.tryReserve(fid, AiUsageService.IMAGE, dailyLimit);
    }

    /** Rend la génération réservée quand le fournisseur a échoué (le commerçant ne la perd pas). */
    public void release(Long fid) {
        usage.release(fid, AiUsageService.IMAGE);
    }

    /** Retourne les octets de l'image (PNG/JPEG) générée à partir du prompt. */
    public byte[] generate(String prompt) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", imageModel);
        body.put("prompt", prompt);
        body.put("n", 1);
        body.put("response_format", "b64_json");
        String url = props.baseUrl().replaceAll("/+$", "") + "/images/generations";
        try {
            RestClient.RequestBodySpec req = http.post().uri(url)
                    .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON);
            if (props.apiKey() != null && !props.apiKey().isBlank()) {
                req = req.header("Authorization", "Bearer " + props.apiKey());
            }
            String raw = req.body(MAPPER.writeValueAsString(body)).retrieve().body(String.class);
            JsonNode b64 = MAPPER.readTree(raw).path("data").path(0).path("b64_json");
            if (b64.isMissingNode() || b64.asText().isBlank()) {
                throw new AssistantUnavailableException("Aucune image générée");
            }
            byte[] bytes = Base64.getDecoder().decode(b64.asText());
            if (bytes.length > MAX_BYTES) throw new AssistantUnavailableException("Image trop volumineuse");
            return bytes;
        } catch (AssistantUnavailableException e) {
            throw e;
        } catch (Exception e) {
            log.warn("logo_image_generation_failed url={} detail={}", url, e.toString());
            throw new AssistantUnavailableException("Génération d'image indisponible", e);
        }
    }
}
