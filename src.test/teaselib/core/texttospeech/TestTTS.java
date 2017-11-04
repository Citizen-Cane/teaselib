package teaselib.core.texttospeech;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Citizen-Cane
 *
 */
public class TestTTS extends TextToSpeechImplementation {
    static final String SDK_NAME = "testtts";

    final List<Voice> voices;

    final Map<String, String> phonemes = new HashMap<>();

    public TestTTS(Voice... voices) {
        this.voices = new ArrayList<>(Arrays.asList(voices));
    }

    public void addVoices(Voice... voices) {
        for (Voice voice : voices) {
            this.voices.add(voice);
        }
    }

    @Override
    public List<Voice> getVoices() {
        return voices;
    }

    @Override
    public String sdkName() {
        return SDK_NAME;
    }

    @Override
    public String phonemeAlphabetName() {
        return UPS;
    }

    @Override
    public void addLexiconEntry(String locale, String word, int partOfSpeech, String pronunciation) {
        phonemes.put(normalized(locale) + "." + word, pronunciation);
    }

    public String getEntry(String locale, String word) {
        return phonemes.get(normalized(locale) + "." + word);
    }

    private static String normalized(String locale) {
        if (locale.contains("-")) {
            return locale;
        } else {
            return locale + "-" + locale;
        }
    }

    @Override
    public void setVoice(Voice voice) { // Ignore
    }

    @Override
    public void speak(String prompt) { // Ignore
    }

    @Override
    public String speak(String prompt, String wav) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void stop() { // Ignore
    }

    @Override
    public void dispose() { // Ignore
    }
}
