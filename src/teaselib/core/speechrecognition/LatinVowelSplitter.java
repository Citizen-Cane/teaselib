package teaselib.core.speechrecognition;

import java.util.ArrayList;
import java.util.List;

/**
 * Split words into syllables by removing any non vowel characters. Doing so
 * doesn't yield accurate, but usually good enough results.
 * <p>
 * Note that the split doesn't return actual syllables, just vowels.
 *
 */
public class LatinVowelSplitter extends BasicSplitter {

    private static final String VOCALS = "aeiouy‰ˆ¸ÈË‡";
    private static final String ALL_VOCALS = VOCALS.toLowerCase() + VOCALS.toUpperCase();

    public LatinVowelSplitter(int minimumNumberOfVowelsForHypothesisRecognition) {
        super(minimumNumberOfVowelsForHypothesisRecognition);
    }

    @Override
    protected String[] split(String text) {
        String[] split = text.split("[^" + ALL_VOCALS + "]|[\\s]");
        List<String> syllables = new ArrayList<String>();
        for (String string : split) {
            if (!string.isEmpty()) {
                syllables.add(string);
            }
        }
        String[] array = new String[syllables.size()];
        return syllables.toArray(array);
    }

}
