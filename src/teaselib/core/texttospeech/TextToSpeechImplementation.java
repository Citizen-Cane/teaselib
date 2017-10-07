package teaselib.core.texttospeech;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class TextToSpeechImplementation {
    private static final Logger logger = LoggerFactory.getLogger(TextToSpeechImplementation.class);

    public abstract void getVoices(Map<String, Voice> voices);

    public abstract void setVoice(Voice voice);

    public abstract void speak(String prompt);

    public abstract String speak(String prompt, String wav);

    /**
     * Stop current speech (if any)
     */
    public abstract void stop();

    public abstract void dispose();

    protected String[] hints = null;

    public void setHints(String... hints) {
        this.hints = hints;
    }

    public String[] getHints() {
        return hints;
    }

    @Override
    protected void finalize() throws Throwable {
        try {
            dispose();
        } catch (Throwable t) {
            logger.error(t.getMessage(), t);
        }
        super.finalize();
    }

    public abstract String sdkName();

    public abstract void addLexiconEntry(String locale, String word, String pronunciation);

    public abstract String phonemeAlphabetName();
}
