package ma.codexa.troco.service.assistant;

import java.util.List;
import java.util.Map;

/** Réponse de l'assistant : texte, actions déjà exécutées ou échouées, et action en attente de confirmation. */
public record AssistantReply(String reply, List<ActionResult> actions) {

    /**
     * @param status   {@code done} | {@code failed} | {@code pending}
     * @param actionId identifiant à confirmer ou annuler, seulement si {@code pending}
     * @param undoId   identifiant pour annuler une action déjà faite, quand elle est annulable
     * @param display  texte court sans donnée sensible, utilisé par l'interface pour décrire l'action
     */
    public record ActionResult(String tool, String status, String actionId, String undoId,
                               Map<String, String> display, String error) {

        static ActionResult done(String tool, Map<String, String> display, String undoId) {
            return new ActionResult(tool, "done", null, undoId, display, null);
        }

        static ActionResult failed(String tool, String error) {
            return new ActionResult(tool, "failed", null, null, Map.of(), error);
        }

        static ActionResult pending(String tool, String actionId, Map<String, String> display) {
            return new ActionResult(tool, "pending", actionId, null, display, null);
        }
    }

    static AssistantReply text(String reply) {
        return new AssistantReply(reply, List.of());
    }
}
