package ma.codexa.troco.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Actions de l'assistant (appels de fonctions). Désactivées par défaut : {@code enabled} sert d'interrupteur
 * d'urgence, sans toucher au chat de conseil.
 */
@ConfigurationProperties(prefix = "app.assistant.tools")
public record AssistantToolsProperties(
        boolean enabled,
        int maxRounds,
        int maxWritesPerTurn,
        int dailyWriteLimit,
        int pendingTtlMinutes
) {
    public AssistantToolsProperties {
        if (maxRounds <= 0) maxRounds = 4;
        if (maxWritesPerTurn <= 0) maxWritesPerTurn = 5;
        if (dailyWriteLimit <= 0) dailyWriteLimit = 40;
        if (pendingTtlMinutes <= 0) pendingTtlMinutes = 10;
    }
}
