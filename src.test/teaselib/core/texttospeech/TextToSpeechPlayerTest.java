package teaselib.core.texttospeech;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import java.io.File;
import java.io.IOException;
import java.util.Locale;
import java.util.Map;

import org.junit.Test;

import teaselib.Actor;
import teaselib.core.Configuration;
import teaselib.core.texttospeech.Voice.Gender;
import teaselib.test.DebugSetup;

public class TextToSpeechPlayerTest {
    private static final Voice MR_FOO = new Voice(0, "Foo", "en-uk", "English-UK", Voice.Gender.Male, "Mr.Foo", "CC",
            "TestApi");
    private static final Voice MR_FOO2 = new Voice(0, "Foo2", "en-uk", "English-UK", Voice.Gender.Male, "Mr.Foo2", "CC",
            "TestApi");
    private static final Voice MR_FOO3 = new Voice(0, "Foo3", "en-uk", "English-UK", Voice.Gender.Male, "Mr.Foo3", "CC",
            "TestApi");
    private static final Voice MR_BAR = new Voice(0, "Bar", "en-in", "English-IN", Voice.Gender.Male, "Mr.Bar", "CC",
            "TestApi");
    private static final Voice MR_BAR2 = new Voice(0, "Bar2", "en-in", "English-IN", Voice.Gender.Male, "Mr.Bar2", "CC",
            "TestApi");
    private static final Voice MR_FOOBAR = new Voice(0, "Foobar", "en", "English", Voice.Gender.Male, "Mr.Foobar", "CC",
            "TestApi");
    static final TestTTS testTTS = new TestTTS(MR_FOO, MR_FOO2, MR_FOO3, MR_BAR, MR_BAR2, MR_FOOBAR);

    @Test
    public void testPlainInitialization() {
        Configuration config = DebugSetup.getConfiguration();
        TextToSpeechPlayer tts = new TextToSpeechPlayer(config, testTTS);

        assertEquals(MR_FOO, tts.getVoiceFor(new Actor("Mr.Foo", Gender.Male, Locale.forLanguageTag("en-uk"))));
        assertEquals(MR_BAR, tts.getVoiceFor(new Actor("Mr.Bar", Gender.Male, Locale.forLanguageTag("en-in"))));
        assertEquals(MR_FOOBAR, tts.getVoiceFor(new Actor("Mr.FooBar", Gender.Male, Locale.ENGLISH)));
    }

    @Test
    public void testPreferredAndIgnored() {
        Configuration config = DebugSetup.getConfiguration();
        config.set(TextToSpeechPlayer.Settings.Voices, getClass().getResource("voices.properties").getPath());
        TextToSpeechPlayer tts = new TextToSpeechPlayer(config, testTTS);

        assertEquals(MR_FOO3, tts.getVoiceFor(new Actor("Mr.Foo", Gender.Male, Locale.forLanguageTag("en-uk"))));
        assertEquals(MR_BAR2, tts.getVoiceFor(new Actor("Mr.Bar", Gender.Male, Locale.forLanguageTag("en-in"))));
        assertEquals(MR_FOOBAR, tts.getVoiceFor(new Actor("Mr.FooBar", Gender.Male, Locale.ENGLISH)));
    }

    @Test
    public void testReload() {
        Configuration config = DebugSetup.getConfiguration();
        config.set(TextToSpeechPlayer.Settings.Voices, getClass().getResource("voices.properties").getPath());
        TextToSpeechPlayer tts = new TextToSpeechPlayer(config, testTTS);

        assertEquals(MR_FOO3, tts.getVoiceFor(new Actor("Mr.Foo", Gender.Male, Locale.forLanguageTag("en-uk"))));
        assertEquals(MR_BAR2, tts.getVoiceFor(new Actor("Mr.Bar", Gender.Male, Locale.forLanguageTag("en-in"))));
        assertEquals(MR_FOOBAR, tts.getVoiceFor(new Actor("Mr.FooBar", Gender.Male, Locale.ENGLISH)));

        config.set(TextToSpeechPlayer.Settings.Voices, getClass().getResource("voices2.properties").getPath());
        tts.reload();

        assertEquals(MR_FOO2, tts.getVoiceFor(new Actor("Mr.Foo", Gender.Male, Locale.forLanguageTag("en-uk"))));
        assertEquals(MR_BAR, tts.getVoiceFor(new Actor("Mr.Bar", Gender.Male, Locale.forLanguageTag("en-in"))));
        assertEquals(MR_FOOBAR, tts.getVoiceFor(new Actor("Mr.FooBar", Gender.Male, Locale.ENGLISH)));
    }

    @Test
    public void testPronountiationConfig() throws IOException {
        Configuration config = DebugSetup.getConfiguration();
        config.set(TextToSpeechPlayer.Settings.Voices, getClass().getResource("pronounciation").getPath());

        PronounciationDictionary dict = new PronounciationDictionary(
                new File(config.get(TextToSpeechPlayer.Settings.Voices)));

        Map<String, String> test = dict.pronounciations("sapi", "Microsoft", "en-en", "MSZira");

        assertEquals("topLevelValue", test.get("languageTopLevelKey"));
        assertEquals("sapiValue", test.get("languageSapiKey"));
        assertEquals("vendorMicrosoftValue", test.get("languageMicrosoftKey"));
        assertEquals("voiceZiraValue", test.get("voiceZiraKey"));

        assertEquals("overriddenByVoiceZiraValue2", test.get("languageTopLevelKey2"));
    }

    @Test
    public void testPronountiationCorrection() throws IOException {
        Configuration config = DebugSetup.getConfiguration();
        config.set(TextToSpeechPlayer.Settings.Voices, getClass().getResource("pronounciation").getPath());

        PronounciationDictionary dict = new PronounciationDictionary(
                new File(config.get(TextToSpeechPlayer.Settings.Voices)));

        Voice voice = new Voice(0, "MSZira", "en-us", "American English", Voice.Gender.Female, "MS Zira", "Microsoft",
                "sapi");
        assertNotEquals("The quick brown fox.", dict.correct(voice, "That quieck bruown animal."));
        assertEquals("The quick brown fox.".toLowerCase(), dict.correct(voice, "That quieck bruown animal."));
    }
}
