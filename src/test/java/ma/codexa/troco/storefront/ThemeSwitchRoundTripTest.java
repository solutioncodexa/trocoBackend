package ma.codexa.troco.storefront;

import ma.codexa.troco.entity.Fournisseur;
import ma.codexa.troco.entity.StoreSettings;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Simule aller-retour classic → minimal → classic sans Spring.
 */
class ThemeSwitchRoundTripTest {

    @Test
    void switchingBackRestoresPreviousLook() {
        Fournisseur f = new Fournisseur();
        f.setPrimaryColor("#111111");
        f.setSecondaryColor("#222222");
        StoreSettings s = new StoreSettings();
        s.setThemeKey("classic");
        s.setFontPair("modern_mono");
        s.setRadiusPreset("round");
        s.setAppearanceJson(StoreAppearance.toJson(Map.of("buttonStyle", "outline", "cardStyle", "flat")));
        s.setHeroEnabled(false);
        s.setCategoriesEnabled(true);
        s.setSurMesureEnabled(false);

        Map<String, Object> presets = new LinkedHashMap<>();
        // Switch classic → minimal
        presets.put("classic", ThemePresets.capture(f, s));
        Map<String, Object> minimalDefaults = ThemePresetFactory.defaultsFor("minimal");
        ThemePresets.apply(minimalDefaults, f, s);
        s.setThemeKey("minimal");
        presets.put("minimal", ThemePresets.capture(f, s));

        assertEquals("#171717", f.getPrimaryColor());
        assertEquals("sharp", s.getRadiusPreset());
        assertTrue(s.isHeroEnabled());

        // Switch back → classic
        Map<String, Object> classic = ThemePresets.getOrNull(presets, "classic");
        assertNotNull(classic);
        ThemePresets.apply(classic, f, s);
        s.setThemeKey("classic");

        assertEquals("#111111", f.getPrimaryColor());
        assertEquals("#222222", f.getSecondaryColor());
        assertEquals("modern_mono", s.getFontPair());
        assertEquals("round", s.getRadiusPreset());
        assertFalse(s.isHeroEnabled());
        assertFalse(s.isSurMesureEnabled());
        Map<String, Object> appearance = StoreAppearance.fromJson(s.getAppearanceJson());
        assertEquals("outline", appearance.get("buttonStyle"));
        assertEquals("flat", appearance.get("cardStyle"));
    }
}
