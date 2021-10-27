package teaselib.core.texttospeech.implementation;

import java.util.Collections;
import java.util.List;

import teaselib.core.texttospeech.TextToSpeechImplementation;
import teaselib.core.texttospeech.Voice;

/**
 * @author Citizen-Cane
 *
 */
public class Unsupported extends TextToSpeechImplementation {
    public static final Unsupported Instance = new Unsupported();

    @Override
    public List<Voice> getVoices() {
        return Collections.emptyList();
    }

    @Override
    public void setVoice(Voice voice) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void speak(String prompt) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String speak(String prompt, String wav) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void stop() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void dispose() { // Nothing to do
    }

    @Override
    public String sdkName() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addLexiconEntry(String locale, String word, int partOfSpeech, String pronunciation) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String phonemeAlphabetName() {
        throw new UnsupportedOperationException();
    }
}
