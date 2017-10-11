package teaselib.core.texttospeech;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class TextToSpeechImplementation {
    private static final Logger logger = LoggerFactory.getLogger(TextToSpeechImplementation.class);

    static int SPPS_Unknown = 0;
    static int SPPS_Noun = 0x1000;
    static int SPPS_Verb = 0x2000;
    static int SPPS_Modifier = 0x3000;
    static int SPPS_Function = 0x4000;
    static int SPPS_Interjection = 0x5000;
    static int SPPS_Noncontent = 0x6000;
    static int SPPS_SuppressWord = 0xf000;

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

    public abstract void addLexiconEntry(String locale, String word, int partOfSpeech, String pronunciation);

    public abstract String phonemeAlphabetName();
}
