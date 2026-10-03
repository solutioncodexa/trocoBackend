package ma.codexa.troco.service.assistant;

import java.util.List;

/** Abstraction du fournisseur LLM : l'assistant ne dépend que de cette interface. */
public interface ChatModelClient {

    /**
     * @return le texte de la réponse
     * @throws AssistantUnavailableException si le fournisseur est injoignable ou répond mal
     */
    String complete(List<ChatMessage> messages);
}
