package teaselib.core.texttospeech;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class TextToSpeechImplementation {
    private static final Logger logger = LoggerFactory.getLogger(TextToSpeechImplementation.class);

    public static final String IPA = "ipa";
    public static final String UPS = "ups";

    // TODO Generalize this copy of Windows SAPI flags
    static int SPPS_Unknown = 0;
    static int SPPS_Noun = 0x1000;
    static int SPPS_Verb = 0x2000;
    static int SPPS_Modifier = 0x3000;
    static int SPPS_Function = 0x4000;
    static int SPPS_Interjection = 0x5000;
    static int SPPS_Noncontent = 0x6000;
    static int SPPS_SuppressWord = 0xf000;

    public abstract List<Voice> getVoices();

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

    public void setPhoneticDictionary(PronunciationDictionary pronunciationDictionary) throws IOException {
        Map<String, Map<String, String>> phonemes = pronunciationDictionary.pronunciations(sdkName(),
                phonemeAlphabetName());
        for (Entry<String, Map<String, String>> entry : phonemes.entrySet()) {
            String locale = entry.getKey();
            Map<String, String> locale2Dictionary = entry.getValue();
            for (Entry<String, String> dictionary : locale2Dictionary.entrySet()) {
                String word = dictionary.getKey();
                String pronunciation = dictionary.getValue();
                // TODO Define part of speech in phoneme dictionary instead of setting all flags
                int partOfSpeech = SPPS_Noun | SPPS_Verb | SPPS_Modifier | SPPS_Function | SPPS_Interjection;
                addLexiconEntry(locale, word, partOfSpeech, pronunciation);
            }
        }
    }

}
