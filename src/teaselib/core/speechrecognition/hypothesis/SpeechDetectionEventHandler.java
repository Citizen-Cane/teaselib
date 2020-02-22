package teaselib.core.speechrecognition.hypothesis;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import teaselib.core.events.Event;
import teaselib.core.speechrecognition.Confidence;
import teaselib.core.speechrecognition.Rule;
import teaselib.core.speechrecognition.SpeechRecognitionEvents;
import teaselib.core.speechrecognition.events.AudioLevelUpdatedEventArgs;
import teaselib.core.speechrecognition.events.AudioSignalProblemOccuredEventArgs;
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

    public final SpeechRecognitionEvents eventSink;
    public final SpeechRecognitionEvents eventForwardingSource;

    private final Event<SpeechRecognitionStartedEventArgs> recognitionStarted;
    private final Event<AudioSignalProblemOccuredEventArgs> audioSignalProblemOccured;
    private final Event<AudioLevelUpdatedEventArgs> audioLevelUpdated;
    private final Event<SpeechRecognizedEventArgs> speechDetected;
    private final Event<SpeechRecognizedEventArgs> recognitionRejected;
    private final Event<SpeechRecognizedEventArgs> recognitionCompleted;

    private PromptSplitter vowelSplitter = new LatinVowelSplitter(HypothesisMinimumNumberOfVowelsDefault);
    private PromptSplitter wordSplitter = new WordSplitter(HypothesisMinimumNumberOfWordsDefault);

    private Confidence expectedConfidence = Confidence.Default;
    private boolean enabled = false;
    private int choiceCount = 0;
    private PromptSplitter promptSplitter = null;
    private int minimumForHypothesisRecognition = 0;
    Rule hypothesisResult;

    public SpeechDetectionEventHandler(SpeechRecognitionEvents eventForwardingSource) {
        super();
        this.eventSink = new SpeechRecognitionEvents();
        this.eventForwardingSource = eventForwardingSource;

        this.recognitionStarted = recognitionStarted();
        this.audioSignalProblemOccured = audioSignalProblemOccured();
        this.audioLevelUpdated = audioLevelUpdated();
        this.speechDetected = speechDetected();
        this.recognitionRejected = recognitionRejected();
        this.recognitionCompleted = recognitionCompleted();
    }

    public void addEventListeners() {
        eventSink.recognitionStarted.add(recognitionStarted);
        eventSink.audioSignalProblemOccured.add(audioSignalProblemOccured);
        eventSink.audioLevelUpdated.add(audioLevelUpdated);
        eventSink.speechDetected.add(speechDetected);
        eventSink.recognitionRejected.add(recognitionRejected);
        eventSink.recognitionCompleted.add(recognitionCompleted);
    }

    public void removeEventListeners() {
        eventSink.recognitionStarted.remove(recognitionStarted);
        eventSink.audioSignalProblemOccured.remove(audioSignalProblemOccured);
        eventSink.audioLevelUpdated.remove(audioLevelUpdated);
        eventSink.speechDetected.remove(speechDetected);
        eventSink.recognitionRejected.remove(recognitionRejected);
        eventSink.recognitionCompleted.remove(recognitionCompleted);
    }

    public void enable(boolean enable) {
        this.enabled = enable;
    }

    public void setChoices(List<String> choices) {
        choiceCount = choices.size();
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

    private Event<SpeechRecognitionStartedEventArgs> recognitionStarted() {
        return eventArgs -> {
            if (!enabled) {
                return;
            } else {
                hypothesisResult = null;
                fireRecognitionStartedEvent(eventArgs);
            }
        };
    }

    private Event<AudioSignalProblemOccuredEventArgs> audioSignalProblemOccured() {
        return this::fireAudioSignalProblemOccuredEvent;
    }

    private Event<AudioLevelUpdatedEventArgs> audioLevelUpdated() {
        return this::fireAudioLevelUpdatedEvent;
    }

    private Event<SpeechRecognizedEventArgs> speechDetected() {
        return eventArgs -> {
            if (!enabled) {
                return;
            } else {
                for (Rule result : eventArgs.result) {
                    if (logger.isInfoEnabled()) {
                        logger.info("rules \n{}", result.prettyPrint());
                    }
                    if (acceptHypothesis(result)) {
                        logger.info("Considering {}", result);
                        hypothesisResult = result;
                    } else {
                        Rule resultWithChoicePropability = result.withDistinctChoiceProbability(choiceCount);
                        if (acceptHypothesis(resultWithChoicePropability)) {
                            logger.info("Considering choice-propabilty-measured result {}",
                                    resultWithChoicePropability);
                            hypothesisResult = resultWithChoicePropability;
                        }
                    }
                }
                fireSpeechDetectedEvent(eventArgs);
            }
        };
    }

    private boolean acceptHypothesis(Rule rule) {
        // TODO Check number of choices equal or higher to support multiple choices
        return hypothesisResult == null //
                || rule.ruleIndex != hypothesisResult.ruleIndex //
                || !rule.choiceIndices.equals(hypothesisResult.choiceIndices) //
                || promptSplitter.count(rule.text) > promptSplitter.count(hypothesisResult.text) //
                || rule.hasHigherProbabilityThan(hypothesisResult);
    }

    private Event<SpeechRecognizedEventArgs> recognitionRejected() {
        return eventArgs -> {
            if (!enabled) {
                return;
            } else if (hypothesisResult == null) {
                fireRecognitionRejectedEvent(eventArgs);
                return;
            } else if (hypothesisIsAcceptable()) {
                eventArgs.consumed = true;
                Rule elevatedRule = new Rule(hypothesisResult, expectedConfidence);
                elevatedRule.children.addAll(hypothesisResult.children);
                logger.info(
                        "Forwarding rejected recognition event {} with accepted hypothesis {} as elevated result {}",
                        eventArgs, hypothesisResult, elevatedRule);

                fireRecognitionCompletedEvent(elevatedRule);
            } else {
                logger.info("rules \n{}", hypothesisResult.prettyPrint());
                fireRecognitionRejectedEvent(eventArgs);
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

    /*
     * Fixed StackOverflow caused by rejected speech rule accepted again
     * 
     * + rule was rejected by firing rejected event downstream, but all events are are processed by both hypothesis
     * handler and input method -> hypothesis handler detects this situation
     * 
     * It's a workaround, the right solution would be to separate events -> hypothesis handler receives events from
     * recognizer and passes them on through a second event system to input method - make sr events private to see who's
     * consuming them
     * 
     * TODO Split event source in hypothesis sink and forward source
     */
    private Event<SpeechRecognizedEventArgs> recognitionCompleted() {
        return eventArgs -> {
            Rule recognitionCompletedResult = eventArgs.result[0];

            if (recognitionCompletedResult.confidence.isLowerThan(expectedConfidence) //
                    && hypothesisResult != null //
                    && hypothesisResult.ruleIndex == recognitionCompletedResult.ruleIndex //
                    && !confidenceIsHighEnough(recognitionCompletedResult, expectedConfidence) //
                    && hypothesisIsAcceptable()) {
                eventArgs.consumed = true;

                Rule elevatedRule = new Rule(recognitionCompletedResult, expectedConfidence);
                elevatedRule.children.addAll(recognitionCompletedResult.children);
                logger.info("Replacing recognition result {} of accepted hypothesis {} as elevated result {}",
                        recognitionCompletedResult, hypothesisResult, elevatedRule);

                fireRecognitionCompletedEvent(elevatedRule);
            } else {
                fireRecognitionCompletedEvent(eventArgs);
            }
        };
    }

    private static boolean confidenceIsHighEnough(Rule result, Confidence confidence) {
        return result.confidence.probability >= confidence.probability;
    }

    private void fireRecognitionStartedEvent(SpeechRecognitionStartedEventArgs eventArgs) {
        eventForwardingSource.recognitionStarted.run(eventArgs);
    }

    private void fireAudioSignalProblemOccuredEvent(AudioSignalProblemOccuredEventArgs eventArgs) {
        eventForwardingSource.audioSignalProblemOccured.run(eventArgs);
    }

    private void fireAudioLevelUpdatedEvent(AudioLevelUpdatedEventArgs eventArgs) {
        eventForwardingSource.audioLevelUpdated.run(eventArgs);
    }

    private void fireSpeechDetectedEvent(SpeechRecognizedEventArgs eventArgs) {
        eventForwardingSource.speechDetected.run(eventArgs);
    }

    private void fireRecognitionRejectedEvent(SpeechRecognizedEventArgs eventArgs) {
        eventForwardingSource.recognitionRejected.run(eventArgs);
    }

    private void fireRecognitionCompletedEvent(SpeechRecognizedEventArgs eventArgs) {
        eventForwardingSource.recognitionCompleted.run(eventArgs);
    }

    private void fireRecognitionCompletedEvent(Rule result) {
        eventForwardingSource.recognitionCompleted.run(new SpeechRecognizedEventArgs(result));
    }

    public Rule getHypothesis() {
        return hypothesisResult;
    }

}
