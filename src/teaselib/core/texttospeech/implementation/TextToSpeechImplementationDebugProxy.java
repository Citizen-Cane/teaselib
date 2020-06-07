/**
 * 
 */
package teaselib.core.texttospeech.implementation;

import java.io.IOException;
import java.util.List;

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
    public String sdkName() {
        return tts.sdkName();
    }

    @Override
    public String phonemeAlphabetName() {
        return tts.phonemeAlphabetName();
    }

    @Override
    public void addLexiconEntry(String locale, String word, int partOfSpeech, String pronunciation) {
        tts.addLexiconEntry(locale, word, partOfSpeech, pronunciation);
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
    public List<Voice> getVoices() {
        return tts.getVoices();
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
    public String speak(String prompt, String wav) throws IOException {
        return tts.speak(prompt, wav);
    }

    @Override
    public void stop() {
        tts.stop();
    }

    @Override
    public void dispose() {
        // Dispose only this, otherwise finalize() would dispose tts impl twice
    }

}
