package teaselib.core.speechrecognition.hypothesis;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import teaselib.core.events.Event;
import teaselib.core.speechrecognition.Confidence;
import teaselib.core.speechrecognition.Rule;
import teaselib.core.speechrecognition.SpeechRecognition;
import teaselib.core.speechrecognition.SpeechRecognitionControl;
import teaselib.core.speechrecognition.SpeechRecognitionResult;
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
    private final Event<SpeechRecognitionControl, SpeechRecognitionStartedEventArgs> recognitionStarted;
    private final Event<SpeechRecognitionControl, SpeechRecognizedEventArgs> speechDetected;
    private final Event<SpeechRecognitionControl, SpeechRecognizedEventArgs> recognitionRejected;
    private final Event<SpeechRecognitionControl, SpeechRecognizedEventArgs> recognitionCompleted;

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

    public void enable(boolean enable) {
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

    private Event<SpeechRecognitionControl, SpeechRecognitionStartedEventArgs> recognitionStarted() {
        return (sender, eventArgs) -> {
            if (!enabled) {
                return;
            } else {
                hypothesisResult = null;
            }
        };
    }

    private Event<SpeechRecognitionControl, SpeechRecognizedEventArgs> speechDetected() {
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
                || result.rule.id != hypothesisResult.rule.id //
                || promptSplitter.count(result.text) > promptSplitter.count(hypothesisResult.text) //
                || result.hasHigherProbabilityThan(hypothesisResult);
    }

    private Event<SpeechRecognitionControl, SpeechRecognizedEventArgs> recognitionRejected() {
        return (sender, eventArgs) -> {
            if (!enabled) {
                return;
            } else if (hypothesisResult == null) {
                return;
            } else if (hypothesisIsAcceptable()) {
                eventArgs.consumed = true;
                Rule elevatedRule = new Rule(hypothesisResult.rule.name, hypothesisResult.rule.text,
                        hypothesisResult.rule.id, hypothesisResult.rule.fromElement, hypothesisResult.rule.toElement,
                        expectedConfidence.probability, expectedConfidence);
                elevatedRule.children.addAll(hypothesisResult.rule.children);
                SpeechRecognitionResult elevatedResult = new SpeechRecognitionResult(hypothesisResult.text,
                        elevatedRule);
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
            if (hypothesisResult.rule.probability < reducedProbability) {
                excuseReducedPropability(reducedProbability);
                return false;
            }
        } else {
            double reducedPropability = expectedConfidence.lower().probability;
            if (hypothesisResult.rule.probability < reducedPropability) {
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
                hypothesisResult.text, hypothesisResult.rule.probability, expectedConfidence, reducedProbability);
    }

    private Event<SpeechRecognitionControl, SpeechRecognizedEventArgs> recognitionCompleted() {
        return (sender, eventArgs) -> {
            SpeechRecognitionResult recognitionCompletedResult = eventArgs.result[0];

            if (recognitionCompletedResult.rule.confidence.isLowerThan(expectedConfidence) //
                    && hypothesisResult != null //
                    && hypothesisResult.rule.id == recognitionCompletedResult.rule.id //
                    && !confidenceIsHighEnough(recognitionCompletedResult, expectedConfidence) //
                    && hypothesisIsAcceptable()) {
                eventArgs.consumed = true;

                Rule elevatedRule = new Rule(recognitionCompletedResult.rule.name, recognitionCompletedResult.rule.text,
                        recognitionCompletedResult.rule.id, recognitionCompletedResult.rule.fromElement,
                        recognitionCompletedResult.rule.toElement, expectedConfidence.probability, expectedConfidence);
                elevatedRule.children.addAll(recognitionCompletedResult.rule.children);
                SpeechRecognitionResult elevatedResult = new SpeechRecognitionResult(recognitionCompletedResult.text,
                        elevatedRule);
                logger.info("Replacing recognition result {} of accepted hypothesis {} as elevated result {}",
                        recognitionCompletedResult, hypothesisResult, elevatedResult);

                fireRecognitionCompletedEvent(sender, elevatedResult);
            }
        };
    }

    private static boolean confidenceIsHighEnough(SpeechRecognitionResult result, Confidence confidence) {
        return result.rule.confidence.probability >= confidence.probability;
    }

    private void fireRecognitionCompletedEvent(SpeechRecognitionControl sender, SpeechRecognitionResult result) {
        SpeechRecognizedEventArgs recognitionCompletedEventArgs = new SpeechRecognizedEventArgs(result);
        speechRecognizer.events.recognitionCompleted.run(sender, recognitionCompletedEventArgs);
    }
}
