package ma.codexa.troco.service.assistant;

/** Message d'une conversation ({@code role} : system | user | assistant). */
public record ChatMessage(String role, String content) {
}
