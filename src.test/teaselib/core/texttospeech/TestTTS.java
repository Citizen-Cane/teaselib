/**
 * 
 */
package teaselib.core.texttospeech;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author Citizen-Cane
 *
 */
public class TestTTS extends TextToSpeechImplementation {
    final Set<Voice> voices;

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
