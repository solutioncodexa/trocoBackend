package ma.codexa.troco.service.assistant;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.Map;

/**
 * Action que l'assistant peut déclencher. Le niveau ({@link Tier}) et les droits sont appliqués par le code, jamais
 * par le modèle : une action {@code CONFIRM} n'est exécutée qu'après accord explicite du commerçant.
 */
public interface AssistantTool {

    enum Tier {
        /** Lecture seule, exécutée aussitôt. */
        READ,
        /** Écriture limitée et réversible, exécutée aussitôt et journalisée. */
        AUTO,
        /** Écriture à fort impact : proposée au commerçant, exécutée seulement s'il confirme. */
        CONFIRM
    }

    String name();

    String description();

    /** Schéma JSON des paramètres (sous-ensemble simple : type, properties, required, enum, description). */
    Map<String, Object> parameters();

    Tier tier();

    /** Permission fine requise (STAFF), ou {@code null}. L'administrateur a toutes les permissions. */
    default String permission() {
        return null;
    }

    /** Réservé au rôle administrateur (réglages de la boutique). */
    default boolean adminOnly() {
        return false;
    }

    /** Résumé affichable de l'action (nom de l'élément concerné…), calculé avant exécution. */
    default Map<String, String> preview(JsonNode args) {
        return Map.of();
    }

    ToolOutcome execute(JsonNode args);
}
