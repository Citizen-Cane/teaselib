package teaselib.core.speechrecognition;

import java.util.ArrayList;
import java.util.List;

public abstract class BasicSplitter implements PromptSplitter {

    private int minimumForHypothesisRecognition;

    public BasicSplitter(int minimumForHypothesisRecognition) {
        super();
        this.minimumForHypothesisRecognition = minimumForHypothesisRecognition;
    }

    @Override
    public int getMinimumForHypothesisRecognition() {
        return minimumForHypothesisRecognition;
    }

    @Override
    public void setMinimumForHypothesisRecognition(int minimumNumberOfWordsForHypothesisRecognition) {
        this.minimumForHypothesisRecognition = minimumNumberOfWordsForHypothesisRecognition;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * teaselib.core.speechrecognition.PromptSplitter#getHypothesisMinimumCount(
     * java.util.List)
     */
    @Override
    public int getMinimumForHypothesisRecognition(List<String> choices) {
        return getMinimumForHypothesisRecognition() + numberOfSameItemsInAnyTwoAtStart(choices);
    }

    private int numberOfSameItemsInAnyTwoAtStart(List<String> choices) {
        if (choices.size() == 1)
            return 0;

        // Split words first in order to not loose consonant infomation
        List<String[]> list = new ArrayList<String[]>();
        for (String choice : choices) {
            String lowerCase = removePunctation(choice).toLowerCase();
            list.add(splitWords(lowerCase));
        }
        int max = 0;
        for (int j = 0; j < list.size(); j++) {
            for (int k = 0; k < list.size(); k++) {
                String[] choice = list.get(j);
                String[] other = list.get(k);
                if (choice == other) {
                    break;
                } else {
                    List<String> common = commonStartSequence(choice, other);
                    int candidate = 0;
                    for (String word : common) {
                        // Now split to vowels/words
                        // as defined in the splitter implementation
                        candidate += split(word).length;
                    }
                    if (candidate > max) {
                        max = candidate;
                    }
                }
            }
        }

        return max;
    }

    List<String> commonStartSequence(String[] list1, String[] list2) {
        List<String> sequence = new ArrayList<String>();
        for (int i = 0; i < list1.length; i++) {
            if (i >= list1.length || i >= list2.length)
                return sequence;
            if (list1[i].compareToIgnoreCase(list2[i]) != 0) {
                return sequence;
            }
            sequence.add(list1[i]);
        }
        return sequence;
    }

    static String[] splitWords(String text) {
        return text.split(" ");
    }

    @Override
    public int count(String text) {
        return split(removePunctation(text)).length;
    }

    private static String removePunctation(String text) {
        text = text.replace(".", " ");
        text = text.replace(":", " ");
        text = text.replace(",", " ");
        text = text.replace(";", " ");
        text = text.replace("!", " ");
        text = text.replace("-", " ");
        text = text.replace("(", " ");
        text = text.replace(")", " ");
        text = text.replace("\"", " ");
        text = text.replace("'", " ");
        text = text.replace("  ", " ");
        text.trim();
        return text;
    }

    protected abstract String[] split(String text);
}
