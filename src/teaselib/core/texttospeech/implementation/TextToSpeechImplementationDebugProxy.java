/**
 * 
 */
package teaselib.core.texttospeech.implementation;

import java.util.Map;

import teaselib.core.texttospeech.TextToSpeechImplementation;
import teaselib.core.texttospeech.Voice;

/**
 * @author Citizen-Cane
 *
 */
public class TextToSpeechImplementationDebugProxy extends TextToSpeechImplementation {
    final TextToSpeechImplementation tts;

    public TextToSpeechImplementationDebugProxy(TextToSpeechImplementation tts) {
        this.tts = tts;
    }

    @Override
    public void setHints(String... hints) {
        tts.setHints(hints);
    }

    @Override
    public String[] getHints() {
        return tts.getHints();
    }

    @Override
    protected void finalize() throws Throwable {
        tts.dispose();
    }

    @Override
    public void getVoices(Map<String, Voice> voices) {
        tts.getVoices(voices);
    }

    @Override
    public void setVoice(Voice voice) {
        tts.setVoice(voice);

    }

    @Override
    public void speak(String prompt) {
        tts.speak(prompt);

    }

    @Override
    public String speak(String prompt, String wav) {
        return tts.speak(prompt, wav);
    }

    @Override
    public void stop() {
        tts.stop();
    }

    @Override
    public void dispose() {
        tts.dispose();
    }

}
