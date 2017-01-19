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
    static final Logger logger = LoggerFactory
            .getLogger(SpeechDetectionEventHandler.class);
    /**
     * The default number of words after which the handler will accept a
     * hypothesis when the confidence is high enough.
     * <p>
     * The first three or four syllables of any prompt are usually detected with
     * a high probability value, so the minimum value should be four.
     */
    private final static int HypothesisMinimumNumberOfWordsDefault = 5;

    /**
     * The default number of vowels after which the handler will accept a
     * hypothesis when the confidence is high enough.
     */
    private final static int HypothesisMinimumNumberOfVowelsDefault = 8;

    private final SpeechRecognition speechRecognizer;
    private final Event<SpeechRecognitionImplementation, SpeechRecognitionStartedEventArgs> recognitionStarted;
    private final Event<SpeechRecognitionImplementation, SpeechRecognizedEventArgs> speechDetected;
    private final Event<SpeechRecognitionImplementation, SpeechRecognizedEventArgs> recognitionRejected;

    private PromptSplitter vowelSplitter = new LatinVowelSplitter(
            HypothesisMinimumNumberOfVowelsDefault);
    private PromptSplitter wordSplitter = new WordSplitter(
            HypothesisMinimumNumberOfWordsDefault);

    private Confidence recognitionConfidence = Confidence.Default;
    private boolean enabled = false;
    private List<String> choices;
    private PromptSplitter promptSplitter = null;
    private int minimumForHypothesisRecognition = 0;
    SpeechRecognitionResult recognitionResult;

    public SpeechDetectionEventHandler(SpeechRecognition speechRecognizer) {
        super();
        this.speechRecognizer = speechRecognizer;
        this.recognitionStarted = recognitionStarted();
        this.speechDetected = speechDetected();
        this.recognitionRejected = recognitionRejected();
    }

    public void addEventListeners() {
        speechRecognizer.events.recognitionStarted.add(recognitionStarted);
        speechRecognizer.events.speechDetected.add(speechDetected);
        speechRecognizer.events.recognitionRejected.add(recognitionRejected);
    }

    public void removeEventListeners() {
        speechRecognizer.events.recognitionStarted.remove(recognitionStarted);
        speechRecognizer.events.speechDetected.remove(speechDetected);
        speechRecognizer.events.recognitionRejected.remove(recognitionRejected);
    }

    void enable(boolean enable) {
        this.enabled = enable;
        // Although this disables event handling in each listener,
        // it doesn't disable event logging by the event source itself
        // TODO Disable event logging since it's very irritating
        // to see speech detection events in the log
    }

    public void setChoices(List<String> choices) {
        this.choices = choices;
        if (vowelSplitter.count(choices.get(0)) > 0) {
            promptSplitter = vowelSplitter;
        } else {
            promptSplitter = wordSplitter;
        }
        minimumForHypothesisRecognition = promptSplitter
                .getMinimumForHypothesisRecognition(choices);
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
            public void run(SpeechRecognitionImplementation sender,
                    SpeechRecognitionStartedEventArgs eventArgs) {
                if (!enabled) {
                    return;
                } else {
                    recognitionResult = null;
                }
            }
        };
    }

    private Event<SpeechRecognitionImplementation, SpeechRecognizedEventArgs> speechDetected() {
        return new Event<SpeechRecognitionImplementation, SpeechRecognizedEventArgs>() {
            @Override
            public void run(SpeechRecognitionImplementation sender,
                    SpeechRecognizedEventArgs eventArgs) {
                if (!enabled) {
                    return;
                } else {
                    for (SpeechRecognitionResult result : eventArgs.result) {
                        if (acceptHypothesis(result)) {
                            logger.info("Considering " + result.toString());
                            recognitionResult = result;
                        }
                    }
                }
            }

            private boolean acceptHypothesis(SpeechRecognitionResult result) {
                if (enoughWordsForHypothesisResult(result)) {
                    if (recognitionResult == null) {
                        return true;
                    } else if (promptSplitter
                            .count(result.text) > promptSplitter
                                    .count(recognitionResult.text)
                            && result.hasHigherProbabilityThan(
                                    recognitionResult)) {
                        return true;
                    }
                }
                return false;
            }

            private boolean enoughWordsForHypothesisResult(
                    SpeechRecognitionResult result) {
                return promptSplitter
                        .count(result.text) >= minimumForHypothesisRecognition;
            }
        };
    }

    private Event<SpeechRecognitionImplementation, SpeechRecognizedEventArgs> recognitionRejected() {
        return new Event<SpeechRecognitionImplementation, SpeechRecognizedEventArgs>() {
            @Override
            public void run(SpeechRecognitionImplementation sender,
                    SpeechRecognizedEventArgs eventArgs) {
                if (!enabled) {
                    return;
                } else if (isRecognizedPrompt()) {
                    eventArgs.consumed = true;
                    logger.info(
                            "Recognized as speech hypothesis -> forwarding as completed");
                    SpeechRecognitionResult[] results = { recognitionResult };
                    SpeechRecognizedEventArgs recognitionCompletedEventArgs = new SpeechRecognizedEventArgs(
                            results);
                    speechRecognizer.events.recognitionCompleted.run(sender,
                            recognitionCompletedEventArgs);
                }
            }

            private boolean isRecognizedPrompt() {
                if (recognitionResult == null) {
                    return false;
                } else if (recognitionResult.confidence.propability < recognitionConfidence.propability) {
                    logger.info("Phrase '" + recognitionResult.text
                            + "' confidence probability="
                            + recognitionResult.propability
                            + " < requested confidence "
                            + recognitionConfidence.toString() + " probability="
                            + recognitionConfidence.propability);
                    return false;
                } else {
                    int count = promptSplitter.count(recognitionResult.text);
                    if (count < minimumForHypothesisRecognition) {
                        logger.info("Phrase '" + recognitionResult.text
                                + "' word detection count=" + count
                                + " < threshold="
                                + minimumForHypothesisRecognition
                                + " is too low to accept hypothesis-based recognition for confidence "
                                + recognitionConfidence.toString());
                        return false;
                    }
                }
                return true;
            }
        };
    }

}
