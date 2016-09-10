package teaselib;

import static org.junit.Assert.assertEquals;

import java.util.Locale;

import org.junit.Test;

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
