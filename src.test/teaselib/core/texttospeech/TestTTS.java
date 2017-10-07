/**
 * 
 */
package teaselib.core.texttospeech;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author Citizen-Cane
 *
 */
public class TestTTS extends TextToSpeechImplementation {
    static final String SDK_NAME = "testtts";

    final Set<Voice> voices;

    final Map<String, String> phonemes = new HashMap<>();

    public TestTTS(Voice... voices) {
        this.voices = new LinkedHashSet<Voice>(Arrays.asList(voices));
    }

    @Override
    public void getVoices(Map<String, Voice> voices) {
        for (Voice voice : this.voices) {
            voices.put(voice.guid, voice);
        }
    }

    @Override
    public String sdkName() {
        return SDK_NAME;
    }

    @Override
    public String phonemeAlphabetName() {
        return "ups";
    }

    @Override
    public void addLexiconEntry(String locale, String word, String pronunciation) {
        phonemes.put(normalized(locale) + "." + word, pronunciation);
    }

    public String getEntry(String locale, String word) {
        return phonemes.get(normalized(locale) + "." + word);
    }

    private String normalized(String locale) {
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
