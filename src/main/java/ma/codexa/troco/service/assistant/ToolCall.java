package ma.codexa.troco.service.assistant;

/** Appel d'outil demandé par le modèle ({@code arguments} : JSON brut). */
public record ToolCall(String id, String name, String arguments) {
}
