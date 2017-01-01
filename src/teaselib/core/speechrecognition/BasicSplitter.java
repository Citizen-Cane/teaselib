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
    public void setMinimumForHypothesisRecognition(
            int minimumNumberOfWordsForHypothesisRecognition) {
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
        return getMinimumForHypothesisRecognition()
                + numberOfSameItemsInAnyTwoAtStart(choices);
    }

    private int numberOfSameItemsInAnyTwoAtStart(List<String> choices) {
        if (choices.size() == 1)
            return 0;
        List<String[]> list = new ArrayList<String[]>();
        for (String choice : choices) {
            String lowerCase = removePunctation(choice).toLowerCase();
            list.add(split(lowerCase));
        }
        int i = 0;
        word: while (true) {
            for (String[] choice : list) {
                for (int j = 0; j < list.size(); j++) {
                    String[] other = list.get(j);
                    if (choice == other) {
                        break;
                    } else if (i > choice.length - 1 || i > other.length - 1)
                        continue;
                    else if (choice[i].equals(other[i])) {
                        i++;
                        continue word;
                    }
                }
            }
            break;
        }
        return i;
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
