package ma.codexa.troco.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Assistant de configuration (chat) — fournisseur LLM compatible API OpenAI
 * ({@code /v1/chat/completions}) : Ollama aujourd'hui, un autre fournisseur plus tard
 * en changeant uniquement {@code baseUrl}, {@code apiKey} et {@code model}.
 */
@ConfigurationProperties(prefix = "app.assistant")
public record AssistantProperties(
        boolean enabled,
        String baseUrl,
        String apiKey,
        String model,
        int timeoutSeconds,
        int maxHistory,
        int maxInputChars,
        int maxOutputTokens,
        double temperature,
        int dailyLimitBasic,
        int dailyLimitPro,
        int dailyLimitBusiness
) {
    public AssistantProperties {
        if (baseUrl == null || baseUrl.isBlank()) baseUrl = "http://ollama:11434/v1";
        if (model == null || model.isBlank()) model = "qwen2.5:7b-instruct";
        if (timeoutSeconds <= 0) timeoutSeconds = 90;
        if (maxHistory <= 0) maxHistory = 10;
        if (maxInputChars <= 0) maxInputChars = 1000;
        if (maxOutputTokens <= 0) maxOutputTokens = 500;
        if (temperature <= 0) temperature = 0.3;
        if (dailyLimitBasic <= 0) dailyLimitBasic = 30;
        if (dailyLimitPro <= 0) dailyLimitPro = 100;
        if (dailyLimitBusiness <= 0) dailyLimitBusiness = 300;
    }

    public int dailyLimitFor(String planCode) {
        String c = planCode == null ? "basic" : planCode.trim().toLowerCase();
        return switch (c) {
            case "pro" -> dailyLimitPro;
            case "business" -> dailyLimitBusiness;
            default -> dailyLimitBasic;
        };
    }
}
