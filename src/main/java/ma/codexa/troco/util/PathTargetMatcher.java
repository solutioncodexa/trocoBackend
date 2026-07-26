package ma.codexa.troco.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Matching simple de chemins pour promos / bandeaux (ex. `/`, `/boutique`, `/page/*`). */
public final class PathTargetMatcher {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private PathTargetMatcher() {}

    public static boolean matches(String targetPathsRaw, String requestPath) {
        List<String> patterns = parse(targetPathsRaw);
        if (patterns.isEmpty()) return true;
        String path = normalize(requestPath);
        for (String pattern : patterns) {
            String p = normalize(pattern);
            if (p.isEmpty()) continue;
            if (p.endsWith("/*")) {
                String prefix = p.substring(0, p.length() - 1); // keep trailing /
                if (path.startsWith(prefix) || path.equals(prefix.substring(0, Math.max(1, prefix.length() - 1)))) {
                    return true;
                }
            } else if (p.equals(path) || (p.equals("/") && (path.isEmpty() || path.equals("/")))) {
                return true;
            }
        }
        return false;
    }

    public static List<String> parse(String raw) {
        List<String> out = new ArrayList<>();
        if (raw == null || raw.isBlank()) return out;
        String trimmed = raw.trim();
        if (trimmed.startsWith("[")) {
            try {
                return MAPPER.readValue(trimmed, new TypeReference<>() {});
            } catch (Exception ignored) {
                /* fallback lines */
            }
        }
        for (String line : trimmed.split("[,\\n]")) {
            String t = line.trim();
            if (!t.isEmpty()) out.add(t);
        }
        return out;
    }

    private static String normalize(String path) {
        if (path == null || path.isBlank()) return "/";
        String p = path.trim();
        int q = p.indexOf('?');
        if (q >= 0) p = p.substring(0, q);
        if (!p.startsWith("/")) p = "/" + p;
        if (p.length() > 1 && p.endsWith("/")) p = p.substring(0, p.length() - 1);
        return p.toLowerCase(Locale.ROOT);
    }
}
