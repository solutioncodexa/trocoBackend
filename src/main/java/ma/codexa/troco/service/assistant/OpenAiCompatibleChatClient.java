package ma.codexa.troco.service.assistant;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import ma.codexa.troco.config.AssistantProperties;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Client pour toute API compatible OpenAI ({@code POST {baseUrl}/chat/completions}) :
 * Gemini ({@code .../v1beta/openai}), Ollama ({@code http://ollama:11434/v1}), OpenAI, Mistral, Groq, etc.
 */
@Slf4j
@Component
public class OpenAiCompatibleChatClient implements ChatModelClient {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final AssistantProperties props;
    private final RestClient http;

    public OpenAiCompatibleChatClient(AssistantProperties props) {
        this.props = props;
        HttpClient jdk = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(jdk);
        factory.setReadTimeout(Duration.ofSeconds(props.timeoutSeconds()));
        this.http = RestClient.builder().requestFactory(factory).build();
    }

    @Override
    public String complete(List<ChatMessage> messages) {
        List<Map<String, Object>> plain = messages.stream()
                .<Map<String, Object>>map(m -> Map.of("role", m.role(), "content", m.content()))
                .toList();
        JsonNode message = post(plain, null).path("choices").path(0).path("message");
        JsonNode content = message.path("content");
        if (content.isMissingNode() || content.asText().isBlank()) {
            throw new AssistantUnavailableException("Réponse vide du modèle");
        }
        return content.asText();
    }

    @Override
    public ModelTurn completeWithTools(List<Map<String, Object>> messages, List<Map<String, Object>> tools) {
        JsonNode message = post(messages, tools).path("choices").path(0).path("message");
        if (message.isMissingNode() || message.isNull()) {
            throw new AssistantUnavailableException("Réponse vide du modèle");
        }
        List<ToolCall> calls = new ArrayList<>();
        for (JsonNode c : message.path("tool_calls")) {
            String name = c.path("function").path("name").asText("");
            if (name.isBlank()) continue;
            calls.add(new ToolCall(c.path("id").asText(""), name, c.path("function").path("arguments").asText("{}")));
        }
        String text = message.path("content").isNull() ? "" : message.path("content").asText("");
        if (calls.isEmpty() && text.isBlank()) {
            throw new AssistantUnavailableException("Réponse vide du modèle");
        }
        return new ModelTurn(text, message, calls);
    }

    private JsonNode post(List<Map<String, Object>> messages, List<Map<String, Object>> tools) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", props.model());
        body.put("messages", messages);
        body.put("temperature", props.temperature());
        body.put("max_tokens", props.maxOutputTokens());
        body.put("stream", false);
        if (tools != null && !tools.isEmpty()) {
            body.put("tools", tools);
            body.put("tool_choice", "auto");
        }

        String url = props.baseUrl().replaceAll("/+$", "") + "/chat/completions";
        try {
            RestClient.RequestBodySpec req = http.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON);
            if (props.apiKey() != null && !props.apiKey().isBlank()) {
                req = req.header("Authorization", "Bearer " + props.apiKey());
            }
            String raw = req.body(MAPPER.writeValueAsString(body)).retrieve().body(String.class);
            return MAPPER.readTree(raw);
        } catch (Exception e) {
            log.warn("Assistant LLM indisponible ({}): {}", url, e.toString());
            throw new AssistantUnavailableException("Assistant indisponible", e);
        }
    }
}
