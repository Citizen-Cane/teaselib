package teaselib.core.speechrecognition;

public class WordSplitter extends BasicSplitter {

    public WordSplitter(int minimumNumberOfWordsForHypothesisRecognition) {
        super(minimumNumberOfWordsForHypothesisRecognition);
    }

    @Override
    public String[] split(String text) {
        return splitWords(text);
    }

}
