package teaselib.core.speechrecognition;

import java.util.List;

public interface PromptSplitter {

    void setMinimumForHypothesisRecognition(
            int minimumNumberOfWordsForHypothesisRecognition);

    int getMinimumForHypothesisRecognition();

    int getHypothesisMinimumCount(List<String> choices);

    int numberOfSameItemsInAnyTwoAtStart(List<String> choices);

    int count(String text);

}
