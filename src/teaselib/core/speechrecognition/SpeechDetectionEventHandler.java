package teaselib.core.speechrecognition;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import teaselib.core.events.Event;
import teaselib.core.speechrecognition.SpeechRecognitionResult.Confidence;
import teaselib.core.speechrecognition.events.SpeechRecognitionStartedEventArgs;
import teaselib.core.speechrecognition.events.SpeechRecognizedEventArgs;

/**
 * Improve speech recognition user experience by just recognizing the first n
 * words. Speaking a longer prompt often results in rejected speech recognition,
 * because the prompt is just too long.
 * <p>
 * By just attempting to recognize the first few words, longer prompts are
 * recognized, while keeping high accuracy.
 * <p>
 * Multiple prompts starting with the same words are taken into account, in
 * order to avoid the wrong prompt being recognized.
 * <p>
 * The minimal word count
 * 
 * @author Citizen-Cane
 *
 */
public class SpeechDetectionEventHandler {
    static final Logger logger = LoggerFactory.getLogger(SpeechDetectionEventHandler.class);
    /**
     * The default number of words after which the handler will accept a
     * hypothesis when the confidence is high enough.
     * <p>
     * The first three or four syllables of any prompt are usually detected with
     * a high probability value, so the minimum value should be four.
     */
    private final static int HypothesisMinimumNumberOfWordsDefault = 3;

    /**
     * The default number of vowels after which the handler will accept a
     * hypothesis when the confidence is high enough.
     */
    private final static int HypothesisMinimumNumberOfVowelsDefault = 3;

    private final SpeechRecognition speechRecognizer;
    private final Event<SpeechRecognitionImplementation, SpeechRecognitionStartedEventArgs> recognitionStarted;
    private final Event<SpeechRecognitionImplementation, SpeechRecognizedEventArgs> speechDetected;
    private final Event<SpeechRecognitionImplementation, SpeechRecognizedEventArgs> recognitionRejected;
    private final Event<SpeechRecognitionImplementation, SpeechRecognizedEventArgs> recognitionCompleted;

    private PromptSplitter vowelSplitter = new LatinVowelSplitter(HypothesisMinimumNumberOfVowelsDefault);
    private PromptSplitter wordSplitter = new WordSplitter(HypothesisMinimumNumberOfWordsDefault);

    private Confidence recognitionConfidence = Confidence.Default;
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
        // Although this disables event handling in each listener,
        // it doesn't disable event logging by the event source itself
        // TODO Disable event logging since it's very irritating
        // to see speech detection events in the log
    }

    public void setChoices(List<String> choices) {
        if (vowelSplitter.count(choices.get(0)) > 0) {
            promptSplitter = vowelSplitter;
        } else {
            promptSplitter = wordSplitter;
        }
        minimumForHypothesisRecognition = promptSplitter.getMinimumForHypothesisRecognition(choices);
    }

    public void setConfidence(Confidence recognitionConfidence) {
        this.recognitionConfidence = recognitionConfidence;
    }

    public Confidence getConfidence() {
        return recognitionConfidence;
    }

    private Event<SpeechRecognitionImplementation, SpeechRecognitionStartedEventArgs> recognitionStarted() {
        return new Event<SpeechRecognitionImplementation, SpeechRecognitionStartedEventArgs>() {
            @Override
            public void run(SpeechRecognitionImplementation sender, SpeechRecognitionStartedEventArgs eventArgs) {
                if (!enabled) {
                    return;
                } else {
                    hypothesisResult = null;
                }
            }
        };
    }

    private Event<SpeechRecognitionImplementation, SpeechRecognizedEventArgs> speechDetected() {
        return new Event<SpeechRecognitionImplementation, SpeechRecognizedEventArgs>() {
            @Override
            public void run(SpeechRecognitionImplementation sender, SpeechRecognizedEventArgs eventArgs) {
                if (!enabled) {
                    return;
                } else {
                    for (SpeechRecognitionResult result : eventArgs.result) {
                        if (acceptHypothesis(result)) {
                            logger.info("Considering " + result.toString());
                            hypothesisResult = result;
                        }
                    }
                }
            }

            private boolean acceptHypothesis(SpeechRecognitionResult result) {
                if (hypothesisResult == null) {
                    return true;
                } else if (result.index != hypothesisResult.index) {
                    return true;
                } else if (promptSplitter.count(result.text) > promptSplitter.count(hypothesisResult.text)) {
                    return true;
                } else if (result.hasHigherProbabilityThan(hypothesisResult)) {
                    return true;
                }
                return false;
            }
        };
    }

    private Event<SpeechRecognitionImplementation, SpeechRecognizedEventArgs> recognitionRejected() {
        return new Event<SpeechRecognitionImplementation, SpeechRecognizedEventArgs>() {
            @Override
            public void run(SpeechRecognitionImplementation sender, SpeechRecognizedEventArgs eventArgs) {
                if (!enabled) {
                    return;
                } else if (isRecognizedPrompt()) {
                    eventArgs.consumed = true;
                    logger.info("Recognized as speech hypothesis -> forwarding as completed");
                    // Raise confidence to requested in order to avoid rejection
                    // in completedEvent handler
                    SpeechRecognitionResult speechRecognitionResult = new SpeechRecognitionResult(
                            hypothesisResult.index, hypothesisResult.text, recognitionConfidence.probability,
                            recognitionConfidence);
                    fireRecognitionCompletedEvent(sender, speechRecognitionResult);
                }
            }

            private boolean isRecognizedPrompt() {
                if (hypothesisResult == null) {
                    return false;
                } else {
                    int count = promptSplitter.count(hypothesisResult.text);
                    if (count < minimumForHypothesisRecognition) {
                        excuseWordDetectionCountTooLow(count);
                        return false;
                    } else if (count == minimumForHypothesisRecognition) {
                        double reducedProbability = (recognitionConfidence.probability
                                + recognitionConfidence.lower().probability) / 2.0;
                        if (hypothesisResult.probability < reducedProbability) {
                            excuseReducedPropability(reducedProbability);
                            return false;
                        }
                    } else if (count > minimumForHypothesisRecognition) {
                        double reducedPropability = recognitionConfidence.lower().probability;
                        if (hypothesisResult.probability < reducedPropability) {
                            excuseReducedPropability(reducedPropability);
                            return false;
                        }

                    }
                }
                return true;
            }

            private void excuseWordDetectionCountTooLow(int count) {
                logger.info("Phrase '" + hypothesisResult.text + "' " + promptSplitter.getClass().getSimpleName()
                        + " detection count=" + count + " < threshold=" + minimumForHypothesisRecognition
                        + " is too low to accept hypothesis-based recognition for confidence "
                        + recognitionConfidence.toString());
            }

            private void excuseReducedPropability(double reducedProbability) {
                logger.info("Phrase '" + hypothesisResult.text + "' confidence probability="
                        + hypothesisResult.probability + " < requested confidence " + recognitionConfidence.toString()
                        + " reduced probability=" + reducedProbability);
            }
        };
    }

    private Event<SpeechRecognitionImplementation, SpeechRecognizedEventArgs> recognitionCompleted() {
        return new Event<SpeechRecognitionImplementation, SpeechRecognizedEventArgs>() {
            @Override
            public void run(SpeechRecognitionImplementation sender, SpeechRecognizedEventArgs eventArgs) {
                SpeechRecognitionResult recognitionCompletedResult = eventArgs.result[0];
                if ((!confidenceIsHighEnough(recognitionCompletedResult, recognitionConfidence)))
                    if (hypothesisResult != null) {
                        if (hypothesisResult.index == recognitionCompletedResult.index
                                && hypothesisResult.probability > eventArgs.result[0].probability) {
                            eventArgs.consumed = true;
                            fireRecognitionCompletedEvent(sender,
                                    new SpeechRecognitionResult(recognitionCompletedResult.index,
                                            recognitionCompletedResult.text, hypothesisResult.probability,
                                            hypothesisResult.confidence));
                        }
                    }
            }

            private boolean confidenceIsHighEnough(SpeechRecognitionResult result, Confidence confidence) {
                return result.confidence.probability >= confidence.probability;
            }
        };
    }

    private void fireRecognitionCompletedEvent(SpeechRecognitionImplementation sender,
            SpeechRecognitionResult speechRecognitionResult) {
        SpeechRecognitionResult[] results = { speechRecognitionResult };
        SpeechRecognizedEventArgs recognitionCompletedEventArgs = new SpeechRecognizedEventArgs(results);
        speechRecognizer.events.recognitionCompleted.run(sender, recognitionCompletedEventArgs);
    }

}
