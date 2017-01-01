package teaselib.core.speechrecognition;

import java.util.List;

public interface PromptSplitter {

    void setMinimumForHypothesisRecognition(
            int minimumNumberOfWordsForHypothesisRecognition);

    int getMinimumForHypothesisRecognition();

    int getMinimumForHypothesisRecognition(List<String> choices);

    int count(String text);

}
