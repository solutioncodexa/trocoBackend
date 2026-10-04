package ma.codexa.troco.service.assistant;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

/** Définition d'outil à base de fonctions, pour déclarer les outils sans une classe par outil. */
record ToolDef(
        String name,
        String description,
        Map<String, Object> parameters,
        Tier tier,
        String permission,
        boolean adminOnly,
        Function<JsonNode, Map<String, String>> previewFn,
        Function<JsonNode, ToolOutcome> run
) implements AssistantTool {

    @Override
    public Map<String, String> preview(JsonNode args) {
        return previewFn == null ? Map.of() : previewFn.apply(args);
    }

    @Override
    public ToolOutcome execute(JsonNode args) {
        return run.apply(args);
    }

    // ───────────── Schémas ─────────────

    static Map<String, Object> str(String description) {
        return Map.of("type", "string", "description", description);
    }

    static Map<String, Object> num(String description) {
        return Map.of("type", "number", "description", description);
    }

    static Map<String, Object> integer(String description) {
        return Map.of("type", "integer", "description", description);
    }

    static Map<String, Object> choice(String description, String... values) {
        return Map.of("type", "string", "description", description, "enum", java.util.List.of(values));
    }

    /** Objet de paramètres ; {@code props} : paires nom, schéma ; {@code required} : noms obligatoires. */
    static Map<String, Object> obj(Map<String, Object> props, String... required) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("type", "object");
        m.put("properties", props);
        if (required.length > 0) m.put("required", java.util.List.of(required));
        return m;
    }
}
