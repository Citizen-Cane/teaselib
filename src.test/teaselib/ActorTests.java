package teaselib;


import java.util.Locale;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ActorTests {

    @Test
    public void testLanguageRegion() {
        Locale en_uk = new Locale("en", "uk");
        assertEquals("en", en_uk.getLanguage());
        assertEquals("UK", en_uk.getCountry());
        assertEquals("en_UK", en_uk.toString());
    }

    @Test
    public void testLangaugeOnly() {
        Locale de = new Locale("de");
        assertEquals("de", de.getLanguage());
        assertEquals("", de.getCountry());
        assertEquals("de", de.toString());
    }
}
