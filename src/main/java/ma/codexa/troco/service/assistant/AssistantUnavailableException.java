package ma.codexa.troco.service.assistant;

/** Le fournisseur LLM est désactivé, injoignable ou a renvoyé une réponse inexploitable. */
public class AssistantUnavailableException extends RuntimeException {

    public AssistantUnavailableException(String message) {
        super(message);
    }

    public AssistantUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
