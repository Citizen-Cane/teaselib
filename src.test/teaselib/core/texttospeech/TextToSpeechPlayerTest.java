package teaselib.core.texttospeech;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static teaselib.core.texttospeech.Voice.Gender.Male;

import java.io.File;
import java.io.IOException;
import java.util.Locale;
import java.util.Map;

import org.junit.BeforeClass;
import org.junit.Test;

import teaselib.Actor;
import teaselib.core.Configuration;
import teaselib.test.DebugSetup;

public class TextToSpeechPlayerTest {
    static final TestTTS testTTS = new TestTTS();

    private static final Voice MR_FOO = new Voice(0, testTTS, "Foo", "en-uk", Male,
            new VoiceInfo("Test", "English-UK", "Mr.Foo"));
    private static final Voice MR_FOO2 = new Voice(0, testTTS, "Foo2", "en-uk", Male,
            new VoiceInfo("Test", "English-UK", "Mr.Foo2"));
    private static final Voice MR_FOO3 = new Voice(0, testTTS, "Foo3", "en-uk", Male,
            new VoiceInfo("Test", "English-UK", "Mr.Foo3"));
    private static final Voice MR_BAR = new Voice(0, testTTS, "Bar", "en-in", Male,
            new VoiceInfo("Test", "English-IN", "Mr.Bar"));
    private static final Voice MR_BAR2 = new Voice(0, testTTS, "Bar2", "en-in", Male,
            new VoiceInfo("Test", "English-IN", "Mr.Bar2"));
    private static final Voice MR_FOOBAR = new Voice(0, testTTS, "Foobar", "en", Male,
            new VoiceInfo("Test", "English", "Mr.Foobar"));

    @BeforeClass
    public static void initTestTTS() {
        testTTS.addVoices(MR_FOO, MR_FOO2, MR_FOO3, MR_BAR, MR_BAR2, MR_FOOBAR);
    }

    @Test
    public void testPlainInitialization() {
        Configuration config = DebugSetup.getConfiguration();
        TextToSpeechPlayer tts = new TextToSpeechPlayer(config, testTTS);

        assertEquals(MR_FOO, tts.getVoiceFor(new Actor("Mr.Foo", Male, Locale.forLanguageTag("en-uk"))));
        assertEquals(MR_BAR, tts.getVoiceFor(new Actor("Mr.Bar", Male, Locale.forLanguageTag("en-in"))));
        assertEquals(MR_FOOBAR, tts.getVoiceFor(new Actor("Mr.FooBar", Male, Locale.ENGLISH)));
    }

    @Test
    public void testPreferredAndIgnored() {
        Configuration config = DebugSetup.getConfiguration();
        config.set(TextToSpeechPlayer.Settings.Voices, getClass().getResource("voices.properties").getPath());
        TextToSpeechPlayer tts = new TextToSpeechPlayer(config, testTTS);

        assertEquals(MR_FOO3, tts.getVoiceFor(new Actor("Mr.Foo", Male, Locale.forLanguageTag("en-uk"))));
        assertEquals(MR_BAR2, tts.getVoiceFor(new Actor("Mr.Bar", Male, Locale.forLanguageTag("en-in"))));
        assertEquals(MR_FOOBAR, tts.getVoiceFor(new Actor("Mr.FooBar", Male, Locale.ENGLISH)));
    }

    @Test
    public void testReload() {
        Configuration config = DebugSetup.getConfiguration();
        config.set(TextToSpeechPlayer.Settings.Voices, getClass().getResource("voices.properties").getPath());
        TextToSpeechPlayer tts = new TextToSpeechPlayer(config, testTTS);

        assertEquals(MR_FOO3, tts.getVoiceFor(new Actor("Mr.Foo", Male, Locale.forLanguageTag("en-uk"))));
        assertEquals(MR_BAR2, tts.getVoiceFor(new Actor("Mr.Bar", Male, Locale.forLanguageTag("en-in"))));
        assertEquals(MR_FOOBAR, tts.getVoiceFor(new Actor("Mr.FooBar", Male, Locale.ENGLISH)));

        config.set(TextToSpeechPlayer.Settings.Voices, getClass().getResource("voices2.properties").getPath());
        tts.reload();

        assertEquals(MR_FOO2, tts.getVoiceFor(new Actor("Mr.Foo", Male, Locale.forLanguageTag("en-uk"))));
        assertEquals(MR_BAR, tts.getVoiceFor(new Actor("Mr.Bar", Male, Locale.forLanguageTag("en-in"))));
        assertEquals(MR_FOOBAR, tts.getVoiceFor(new Actor("Mr.FooBar", Male, Locale.ENGLISH)));
    }

    @Test
    public void testPronountiationConfig() throws IOException {
        Configuration config = DebugSetup.getConfiguration();
        config.set(TextToSpeechPlayer.Settings.Pronunciation, getClass().getResource("pronunciation").getPath());

        PronunciationDictionary dict = new PronunciationDictionary(
                new File(config.get(TextToSpeechPlayer.Settings.Pronunciation)));

        Map<String, String> test = dict.pronunciations(TestTTS.SDK_NAME, "Microsoft", "en-en", "MSZira");

        assertEquals("topLevelValue", test.get("languageTopLevelKey"));
        assertEquals("sapiValue", test.get("languageSapiKey"));
        assertEquals("vendorMicrosoftValue", test.get("languageMicrosoftKey"));
        assertEquals("voiceZiraValue", test.get("voiceZiraKey"));

        assertEquals("overriddenByVoiceZiraValue2", test.get("languageTopLevelKey2"));
    }

    @Test
    public void testPronountiationCorrection() throws IOException {
        Configuration config = DebugSetup.getConfiguration();
        config.set(TextToSpeechPlayer.Settings.Pronunciation, getClass().getResource("pronunciation").getPath());

        PronunciationDictionary dict = new PronunciationDictionary(
                new File(config.get(TextToSpeechPlayer.Settings.Pronunciation)));

        Voice voice = new Voice(0, testTTS, "MSZira", "en-us", Voice.Gender.Female,
                new VoiceInfo("Microsoft", "American English", "MS Zira"));

        assertEquals("the quick brown fox.", dict.correct(voice, "That quieck bruown animal."));
        assertEquals("The quick brown fox.".toLowerCase(), dict.correct(voice, "That quieck bruown animal."));
    }

    @Test
    public void testPhonemeDictionarySetup() throws IOException {
        Configuration config = DebugSetup.getConfiguration();
        config.set(TextToSpeechPlayer.Settings.Pronunciation, getClass().getResource("pronunciation").getPath());

        new TextToSpeechPlayer(config, testTTS);

        assertEquals("madam", testTTS.getEntry("fr", "Madame"));
        assertEquals("madam", testTTS.getEntry("fr-fr", "Madame"));
        assertNull("H EH 1 L OW", testTTS.getEntry("en", "Hello"));
        assertEquals("H EH 1 L OW", testTTS.getEntry("en-au", "Hello"));
        assertNull(testTTS.getEntry("en-uk", "Hello world"));
        assertNull(testTTS.getEntry("fr", "UPS-Ignored"));
    }
}