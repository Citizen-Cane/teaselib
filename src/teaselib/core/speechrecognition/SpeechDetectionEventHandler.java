package teaselib.core.speechrecognition;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import teaselib.core.events.Event;
import teaselib.core.speechrecognition.SpeechRecognitionResult.Confidence;
import teaselib.core.speechrecognition.events.SpeechRecognitionStartedEventArgs;
import teaselib.core.speechrecognition.events.SpeechRecognizedEventArgs;

/**
 * Improve speech recognition user experience by just recognizing the first n words. Speaking a longer prompt often
 * results in rejected speech recognition, because the prompt is just too long.
 * <p>
 * By just attempting to recognize the first few words, longer prompts are recognized, while keeping high accuracy.
 * <p>
 * Multiple prompts starting with the same words are taken into account, in order to avoid the wrong prompt being
 * recognized.
 * <p>
 * The minimal word count
 * 
 * @author Citizen-Cane
 *
 */
public class SpeechDetectionEventHandler {
    static final Logger logger = LoggerFactory.getLogger(SpeechDetectionEventHandler.class);
    /**
     * The default number of words after which the handler will accept a hypothesis when the confidence is high enough.
     * <p>
     * The first three or four syllables of any prompt are usually detected with a high probability value, so the
     * minimum value should be four.
     */
    private static final int HypothesisMinimumNumberOfWordsDefault = 3;

    /**
     * The default number of vowels after which the handler will accept a hypothesis when the confidence is high enough.
     */
    private static final int HypothesisMinimumNumberOfVowelsDefault = 4;

    private final SpeechRecognition speechRecognizer;
    private final Event<SpeechRecognitionImplementation, SpeechRecognitionStartedEventArgs> recognitionStarted;
    private final Event<SpeechRecognitionImplementation, SpeechRecognizedEventArgs> speechDetected;
    private final Event<SpeechRecognitionImplementation, SpeechRecognizedEventArgs> recognitionRejected;
    private final Event<SpeechRecognitionImplementation, SpeechRecognizedEventArgs> recognitionCompleted;

    private PromptSplitter vowelSplitter = new LatinVowelSplitter(HypothesisMinimumNumberOfVowelsDefault);
    private PromptSplitter wordSplitter = new WordSplitter(HypothesisMinimumNumberOfWordsDefault);

    private Confidence expectedConfidence = Confidence.Default;
    private boolean enabled = false;
    private PromptSplitter promptSplitter = null;
    private int minimumForHypothesisRecognition = 0;
    SpeechRecognitionResult hypothesisResult;

    public SpeechDetectionEventHandler(SpeechRecognition speechRecognizer) {
        super();
        this.speechRecognizer = speechRecognizer;
        this.recognitionStarted = recognitionStarted();
        this.speechDetected = speechDetected();
        this.recognitionRejected = recognitionRejected();
        this.recognitionCompleted = recognitionCompleted();
    }

    public void addEventListeners() {
        speechRecognizer.events.recognitionStarted.add(recognitionStarted);
        speechRecognizer.events.speechDetected.add(speechDetected);
        speechRecognizer.events.recognitionRejected.add(recognitionRejected);
        speechRecognizer.events.recognitionCompleted.add(recognitionCompleted);
    }

    public void removeEventListeners() {
        speechRecognizer.events.recognitionStarted.remove(recognitionStarted);
        speechRecognizer.events.speechDetected.remove(speechDetected);
        speechRecognizer.events.recognitionRejected.remove(recognitionRejected);
        speechRecognizer.events.recognitionCompleted.remove(recognitionCompleted);
    }

    void enable(boolean enable) {
        this.enabled = enable;
    }

    public void setChoices(List<String> choices) {
        if (vowelSplitter.count(choices.get(0)) > 0) {
            promptSplitter = vowelSplitter;
        } else {
            promptSplitter = wordSplitter;
        }
        minimumForHypothesisRecognition = promptSplitter.getMinimumForHypothesisRecognition(choices);
    }

    public void setExpectedConfidence(Confidence expectedConfidence) {
        this.expectedConfidence = expectedConfidence;
    }

    public Confidence getexpectedConfidence() {
        return expectedConfidence;
    }

    private Event<SpeechRecognitionImplementation, SpeechRecognitionStartedEventArgs> recognitionStarted() {
        return (sender, eventArgs) -> {
            if (!enabled) {
                return;
            } else {
                hypothesisResult = null;
            }
        };
    }

    private Event<SpeechRecognitionImplementation, SpeechRecognizedEventArgs> speechDetected() {
        return (sender, eventArgs) -> {
            if (!enabled) {
                return;
            } else {
                for (SpeechRecognitionResult result : eventArgs.result) {
                    if (acceptHypothesis(result)) {
                        logger.info("Considering {}", result);
                        hypothesisResult = result;
                    }
                }
            }
        };
    }

    private boolean acceptHypothesis(SpeechRecognitionResult result) {
        return hypothesisResult == null //
                || result.index != hypothesisResult.index //
                || promptSplitter.count(result.text) > promptSplitter.count(hypothesisResult.text) //
                || result.hasHigherProbabilityThan(hypothesisResult);
    }

    private Event<SpeechRecognitionImplementation, SpeechRecognizedEventArgs> recognitionRejected() {
        return (sender, eventArgs) -> {
            if (!enabled) {
                return;
            } else if (hypothesisResult == null) {
                return;
            } else if (hypothesisIsAcceptable()) {
                eventArgs.consumed = true;
                SpeechRecognitionResult elevatedResult = new SpeechRecognitionResult(hypothesisResult.index,
                        hypothesisResult.text, expectedConfidence.probability, expectedConfidence);
                logger.info(
                        "Forwarding rejected recognition event {} with accepted hypothesis {} as elevated result {}",
                        eventArgs, hypothesisResult, elevatedResult);
                fireRecognitionCompletedEvent(sender, elevatedResult);
            }
        };
    }

    private boolean hypothesisIsAcceptable() {
        int count = promptSplitter.count(hypothesisResult.text);
        if (count < minimumForHypothesisRecognition) {
            excuseWordDetectionCountTooLow(count);
            return false;
        } else if (count == minimumForHypothesisRecognition) {
            double reducedProbability = expectedConfidence.reducedProbability();
            if (hypothesisResult.probability < reducedProbability) {
                excuseReducedPropability(reducedProbability);
                return false;
            }
        } else {
            double reducedPropability = expectedConfidence.lower().probability;
            if (hypothesisResult.probability < reducedPropability) {
                excuseReducedPropability(reducedPropability);
                return false;
            }
        }
        return true;
    }

    private void excuseWordDetectionCountTooLow(int count) {
        logger.info(
                "Phrase '{}' {} detection count={} < threshold={} is too low to accept hypothesis-based recognition for confidence {}",
                hypothesisResult.text, promptSplitter.getClass().getSimpleName(), count,
                minimumForHypothesisRecognition, expectedConfidence);
    }

    private void excuseReducedPropability(double reducedProbability) {
        logger.info("Phrase '{}' confidence probability={} < requested confidence {} reduced probability={}",
                hypothesisResult.text, hypothesisResult.probability, expectedConfidence, reducedProbability);
    }

    private Event<SpeechRecognitionImplementation, SpeechRecognizedEventArgs> recognitionCompleted() {
        return (sender, eventArgs) -> {
            SpeechRecognitionResult recognitionCompletedResult = eventArgs.result[0];

            if (recognitionCompletedResult.confidence.isLowerThan(expectedConfidence) //
                    && hypothesisResult != null //
                    && hypothesisResult.index == recognitionCompletedResult.index //
                    && !confidenceIsHighEnough(recognitionCompletedResult, expectedConfidence) //
                    && hypothesisIsAcceptable()) {
                eventArgs.consumed = true;
                SpeechRecognitionResult elevatedResult = new SpeechRecognitionResult(hypothesisResult.index,
                        recognitionCompletedResult.text, expectedConfidence.probability, expectedConfidence);
                logger.info("Replacing recognition result {} of accepted hypothesis {} as elevated result {}",
                        recognitionCompletedResult, hypothesisResult, elevatedResult);
                fireRecognitionCompletedEvent(sender, elevatedResult);
            }
        };
    }

    private boolean confidenceIsHighEnough(SpeechRecognitionResult result, Confidence confidence) {
        return result.confidence.probability >= confidence.probability;
    }

    private void fireRecognitionCompletedEvent(SpeechRecognitionImplementation sender, SpeechRecognitionResult result) {
        SpeechRecognizedEventArgs recognitionCompletedEventArgs = new SpeechRecognizedEventArgs(result);
        speechRecognizer.events.recognitionCompleted.run(sender, recognitionCompletedEventArgs);
    }
}
