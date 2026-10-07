package ma.codexa.troco.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PhoneUtilTest {

    @Test
    void numerosMarocainsConvergentVersLaMemeForme() {
        for (String raw : new String[]{"06 12 34 56 78", "0612345678", "+212612345678", "+212 6 12 34 56 78",
                "00212612345678", "612345678", "212612345678", "+212 (0)612345678", "06.12.34.56.78", "06-12-34-56-78"}) {
            assertEquals("+212612345678", PhoneUtil.normalize(raw), raw);
        }
        assertEquals("+212522123456", PhoneUtil.normalize("05 22 12 34 56"));
    }

    @Test
    void numerosEtrangersEtVidesNeSontPasDeformes() {
        assertEquals("+33612345678", PhoneUtil.normalize("+33 6 12 34 56 78"));
        assertEquals("+33612345678", PhoneUtil.normalize("0033612345678"));
        assertNull(PhoneUtil.normalize("  "));
        assertNull(PhoneUtil.normalize(null));
        assertNull(PhoneUtil.normalize("abc"));
    }

    @Test
    void rechercheEtAncienneCle() {
        assertTrue(PhoneUtil.looksLikePhone("06 12 34"));
        assertFalse(PhoneUtil.looksLikePhone("CMD-2026"));
        assertEquals("612345678", PhoneUtil.searchDigits("0612345678"));
        assertEquals("612345678", PhoneUtil.searchDigits("+212 612345678"));
        assertEquals("0612345678", PhoneUtil.legacyKey("06 12 34 56 78"));
    }
}
