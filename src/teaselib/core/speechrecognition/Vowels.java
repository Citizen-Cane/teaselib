package teaselib.core.speechrecognition;

import java.util.ArrayList;
import java.util.List;

public class Vowels {

    private int minimumNumberOfWordsForHypothesisRecognition;

    public Vowels(int minimumNumberOfWordsForHypothesisRecognition) {
        this.minimumNumberOfWordsForHypothesisRecognition = minimumNumberOfWordsForHypothesisRecognition;
    }

    public int getMinimumNumberOfWordsForHypothesisRecognition() {
        return minimumNumberOfWordsForHypothesisRecognition;
    }

    public void setMinimumNumberOfWordsForHypothesisRecognition(
            int minimumNumberOfWordsForHypothesisRecognition) {
        this.minimumNumberOfWordsForHypothesisRecognition = minimumNumberOfWordsForHypothesisRecognition;
    }

    public int count(String text) {
        String preparatedText = text;
        preparatedText = removePunctation(preparatedText);
        return splitVowels(preparatedText).length;
    }

    public int getHypothesisMinimumCount(List<String> choices) {
        return numberOfSameItemsInAnyTwoAtStart(choices)
                + getMinimumNumberOfWordsForHypothesisRecognition();
    }

    public int numberOfSameItemsInAnyTwoAtStart(List<String> choices) {
        if (choices.size() == 1)
            return 0;
        List<String[]> list = new ArrayList<String[]>();
        for (String choice : choices) {
            String lowerCase = removePunctation(choice).toLowerCase();
            list.add(splitVowels(lowerCase));
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

    public String[] splitVowels(String text) {
        return text.split(" ");
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

}
