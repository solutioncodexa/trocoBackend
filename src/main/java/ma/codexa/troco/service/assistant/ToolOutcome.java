package ma.codexa.troco.service.assistant;

import java.util.Map;

/**
 * Résultat d'un outil : {@code data} est renvoyé au modèle, {@code display} (texte court, sans donnée sensible)
 * est montré au commerçant, {@code undo} (facultatif) remet la boutique dans l'état d'avant l'action.
 */
public record ToolOutcome(Object data, Map<String, String> display, Runnable undo) {

    public static ToolOutcome of(Object data) {
        return new ToolOutcome(data, Map.of(), null);
    }

    public static ToolOutcome of(Object data, Map<String, String> display) {
        return new ToolOutcome(data, display, null);
    }

    public static ToolOutcome of(Object data, Map<String, String> display, Runnable undo) {
        return new ToolOutcome(data, display, undo);
    }
}
