package ma.codexa.troco.service.assistant;

import java.util.List;
import java.util.Map;

/** Abstraction du fournisseur LLM : l'assistant ne dépend que de cette interface. */
public interface ChatModelClient {

    /**
     * @return le texte de la réponse
     * @throws AssistantUnavailableException si le fournisseur est injoignable ou répond mal
     */
    String complete(List<ChatMessage> messages);

    /**
     * Tour de conversation avec outils (format OpenAI : messages et définitions de fonctions en {@code Map}).
     *
     * @throws AssistantUnavailableException si le fournisseur est injoignable, répond mal ou ne gère pas les outils
     */
    default ModelTurn completeWithTools(List<Map<String, Object>> messages, List<Map<String, Object>> tools) {
        throw new AssistantUnavailableException("Appels de fonctions non pris en charge");
    }
}
