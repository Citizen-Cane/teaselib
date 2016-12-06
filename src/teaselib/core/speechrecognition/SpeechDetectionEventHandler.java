package teaselib.core.speechrecognition;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import teaselib.core.events.Event;
import teaselib.core.speechrecognition.SpeechRecognitionResult.Confidence;
import teaselib.core.speechrecognition.events.SpeechRecognitionStartedEventArgs;
import teaselib.core.speechrecognition.events.SpeechRecognizedEventArgs;

/**
 * This was started as an approach to get along with low-quality microphones,
 * but for microphones/speaker configurations it starts to recognize noise as
 * speech - sooner or later.
 * <p>
 * Might still be useful for headphones with integrated microphones, but this
 * claim has yet to be tested.
 * <p>
 * Well, there's one use for un-exactly recognizing phrases: when we don't care
 * about the exact phrase. This is how it is used by the speech recognizer, to
 * fetch results with low confidence for long prompts.
 * 
 * @author Citizen-Cane
 *
 */
public class SpeechDetectionEventHandler {
    static final Logger logger = LoggerFactory
            .getLogger(SpeechDetectionEventHandler.class);
    /**
     * Hypothesis speech recognition is used for longer sentences, as short
     * sentences or single word recognitions are prone to error. In fact, for
     * single word phrases, the recognizer may recognize anything.
     * <p>
     * The first three or four syllables of any prompt are usually detected with
     * a high probability values, so the minimum value should be four.
     */
    private final static int HypothesisMinimumNumberOfWordsDefault = 6;

    /**
     * This adjusts the sensibility of the hypothesis rating. The better the
     * microphone, the higher this value should be.
     */
    final static double HypothesisMinimumAccumulatedWeight = 1.0;

    private final SpeechRecognition speechRecognizer;
    private final Event<SpeechRecognitionImplementation, SpeechRecognitionStartedEventArgs> recognitionStarted;
    private final Event<SpeechRecognitionImplementation, SpeechRecognizedEventArgs> speechDetected;
    private final Event<SpeechRecognitionImplementation, SpeechRecognizedEventArgs> recognitionRejected;

    private List<String> choices;
    SpeechRecognitionResult recognitionResult;

    private int minimumNumberOfWordsForHypothesisRecognition = HypothesisMinimumNumberOfWordsDefault;
    private Confidence recognitionConfidence = Confidence.Default;
    private boolean enabled = false;

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
        speechRecognizer.events.recognitionStarted.add(recognitionStarted);
        speechRecognizer.events.speechDetected.add(speechDetected);
        speechRecognizer.events.recognitionRejected.add(recognitionRejected);
    }

    void enable(boolean enable) {
        this.enabled = enable;
    }

    public void setChoices(List<String> choices) {
        this.choices = choices;
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
                        if (recognitionResult == null || (result
                                .hasHigherProbabilityThan(recognitionResult)
                                || wordCount(result.text) > wordCount(
                                        recognitionResult.text))) {
                            recognitionResult = result;
                        }
                    }
                }
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
                            "Recognized as speech hypothesis -> forwarding event as completed");
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
                    int wordCount = wordCount(recognitionResult.text);
                    int minimumNumberOfWordsRequired = getHypothesisMinimumNumberOfWords(
                            choices);
                    if (wordCount < minimumNumberOfWordsRequired) {
                        logger.info("Phrase '" + recognitionResult.text
                                + "' word detection count=" + wordCount
                                + " < threshold=" + minimumNumberOfWordsRequired
                                + " is too low to accept hypothesis-based recognition for confidence "
                                + recognitionConfidence.toString());
                        return false;
                    }
                }
                return true;
            }
        };
    }

    int getHypothesisMinimumNumberOfWords(List<String> choices) {
        return getMinimumNumberOfWordsForHypothesisRecognition()
                + numberOfSameWordsInAnyTwoChoicesAtStart(choices);
    }

    private static int numberOfSameWordsInAnyTwoChoicesAtStart(
            List<String> choices) {
        if (choices.size() == 1)
            return 0;
        List<String[]> list = new ArrayList<String[]>();
        for (String choice : choices) {
            list.add(removePunctation(choice).toLowerCase().split(" "));
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

    static int wordCount(String text) {
        String preparatedText = text;
        preparatedText = removePunctation(preparatedText);
        return preparatedText.split(" ").length;
    }

    private static String removePunctation(String preparatedText) {
        preparatedText = preparatedText.replace(".", " ");
        preparatedText = preparatedText.replace(":", " ");
        preparatedText = preparatedText.replace(",", " ");
        preparatedText = preparatedText.replace(";", " ");
        preparatedText = preparatedText.replace("!", " ");
        preparatedText = preparatedText.replace("-", " ");
        preparatedText = preparatedText.replace("(", " ");
        preparatedText = preparatedText.replace(")", " ");
        preparatedText = preparatedText.replace("\"", " ");
        preparatedText = preparatedText.replace("'", " ");
        preparatedText = preparatedText.replace("  ", " ");
        preparatedText.trim();
        return preparatedText;
    }

    public int getMinimumNumberOfWordsForHypothesisRecognition() {
        return minimumNumberOfWordsForHypothesisRecognition;
    }

    public void setMinimumNumberOfWordsForHypothesisRecognition(
            int minimumNumberOfWordsForHypothesisRecognition) {
        this.minimumNumberOfWordsForHypothesisRecognition = minimumNumberOfWordsForHypothesisRecognition;
    }
}
