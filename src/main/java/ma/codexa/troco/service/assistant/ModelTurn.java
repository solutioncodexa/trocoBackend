package ma.codexa.troco.service.assistant;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;

/**
 * Réponse du modèle : texte et/ou appels d'outils. {@code rawMessage} est renvoyé tel quel au modèle au tour suivant
 * (certains fournisseurs y attachent des signatures à restituer).
 */
public record ModelTurn(String text, JsonNode rawMessage, List<ToolCall> calls) {
}
