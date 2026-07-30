package ma.codexa.troco.storefront;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ThemePresetsTest {

    @Test
    void roundTripJsonPreservesThemes() {
        Map<String, Object> all = new LinkedHashMap<>();
        all.put("classic", ThemePresetFactory.defaultsFor("classic"));
        all.put("minimal", ThemePresetFactory.defaultsFor("minimal"));
        String json = ThemePresets.toJson(all);
        Map<String, Object> parsed = ThemePresets.parseAll(json);
        assertTrue(parsed.containsKey("classic"));
        assertTrue(parsed.containsKey("minimal"));
        Map<String, Object> minimal = ThemePresets.getOrNull(parsed, "minimal");
        assertNotNull(minimal);
        assertEquals("#171717", minimal.get("primaryColor"));
        assertEquals("sharp", minimal.get("radiusPreset"));
    }

    @Test
    void defaultsDifferByTheme() {
        Map<String, Object> classic = ThemePresetFactory.defaultsFor("classic");
        Map<String, Object> bold = ThemePresetFactory.defaultsFor("bold");
        assertNotEquals(classic.get("primaryColor"), bold.get("primaryColor"));
        assertEquals("editorial_serif", ThemePresetFactory.defaultsFor("elegant").get("fontPair"));
    }
}
