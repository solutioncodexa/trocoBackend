package ma.codexa.troco.service.assistant;

import ma.codexa.troco.security.service.PermissionCheckService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Catalogue des outils de l'assistant, filtré par les droits de la personne connectée. */
@Component
public class AssistantToolRegistry {

    private final Map<String, AssistantTool> byName = new LinkedHashMap<>();
    private final PermissionCheckService permissions;

    public AssistantToolRegistry(StoreAssistantTools storeTools, PermissionCheckService permissions) {
        this.permissions = permissions;
        storeTools.tools().forEach(t -> byName.put(t.name(), t));
    }

    public Optional<AssistantTool> find(String name) {
        return Optional.ofNullable(byName.get(name));
    }

    /** L'utilisateur courant a-t-il le droit d'utiliser cet outil ? Revérifié à chaque exécution. */
    public boolean permitted(AssistantTool tool) {
        if (tool.adminOnly() && !isAdmin()) return false;
        return tool.permission() == null || permissions.currentUserHasPermission(tool.permission());
    }

    /** Définitions envoyées au modèle : uniquement les outils que cette personne peut utiliser. */
    public List<Map<String, Object>> specs() {
        return byName.values().stream()
                .filter(this::permitted)
                .<Map<String, Object>>map(t -> Map.of("type", "function", "function", Map.of(
                        "name", t.name(),
                        "description", t.description(),
                        "parameters", t.parameters())))
                .toList();
    }

    private static boolean isAdmin() {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        return a != null && a.getAuthorities().stream()
                .anyMatch(g -> "ROLE_ADMIN".equals(g.getAuthority()) || "ROLE_SUPER_ADMIN".equals(g.getAuthority()));
    }
}
