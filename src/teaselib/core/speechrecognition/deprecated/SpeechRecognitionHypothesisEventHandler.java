package teaselib.core.speechrecognition.deprecated;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import teaselib.core.events.Event;
import teaselib.core.speechrecognition.SpeechRecognition;
import teaselib.core.speechrecognition.SpeechRecognitionImplementation;
import teaselib.core.speechrecognition.SpeechRecognitionResult;
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
@SuppressWarnings("deprecation")
@Deprecated
public class SpeechRecognitionHypothesisEventHandler {
    static final Logger logger = LoggerFactory
            .getLogger(SpeechRecognitionHypothesisEventHandler.class);
    /**
     * Hypothesis speech recognition is used for longer sentences, as short
     * sentences or single word recognitions are prone to error. In fact, for
     * single word phrases, the recognizer may recognize anything.
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

    private double[] hypothesisAccumulatedWeights;
    String[] hypothesisProgress;

    List<String> choices;
    private int minimumNumberOfWordsForHypothesisRecognition = HypothesisMinimumNumberOfWordsDefault;
    private Confidence recognitionConfidence = Confidence.Default;
    private boolean enabled = false;

    public SpeechRecognitionHypothesisEventHandler(
            SpeechRecognition speechRecognizer) {
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
                if (!enabled)
                    return;
                final int size = choices.size();
                hypothesisAccumulatedWeights = new double[size];
                hypothesisProgress = new String[size];
                for (int i = 0; i < hypothesisAccumulatedWeights.length; i++) {
                    hypothesisAccumulatedWeights[i] = 0;
                    hypothesisProgress[i] = "";
                }
            }
        };
    }

    private Event<SpeechRecognitionImplementation, SpeechRecognizedEventArgs> speechDetected() {
        return new Event<SpeechRecognitionImplementation, SpeechRecognizedEventArgs>() {
            @Override
            public void run(SpeechRecognitionImplementation sender,
                    SpeechRecognizedEventArgs eventArgs) {
                if (!enabled)
                    return;
                // The Microsoft SAPI based SR is supposed to return
                // multiple results, however usually only one entry is
                // returned when hypothesizing a recognition result and
                // multiple choices start with the same words
                SpeechRecognitionResult[] recognitionResults = eventArgs.result;
                if (recognitionResults.length == 1) {
                    // Manually search for all choices that start with the
                    // hypothesis, and add the probability weight for each
                    SpeechRecognitionResult hypothesis = eventArgs.result[0];
                    String hypothesisText = hypothesis.text;
                    final double propabilityWeight = propabilityWeight(
                            hypothesis);
                    for (int index = 0; index < choices.size(); index++) {
                        String choice = choices.get(index).toLowerCase();
                        if (choice.startsWith(hypothesisText.toLowerCase())) {
                            updateHypothesisProgress(index, hypothesisText,
                                    propabilityWeight);
                        }
                    }
                } else {
                    for (SpeechRecognitionResult hypothesis : recognitionResults) {
                        // The first word(s) are usually incorrect,
                        // whereas later hypothesis usually match better
                        double propabilityWeight = propabilityWeight(
                                hypothesis);
                        int index = hypothesis.index;
                        updateHypothesisProgress(index, hypothesis.text,
                                propabilityWeight);
                    }
                }
            }

            private void updateHypothesisProgress(int index,
                    String hypothesisText, double propabilityWeight) {
                // Only update if the hypothesis is progressing,
                // e.g. sort out detection duplicates
                if (hypothesisText.startsWith(hypothesisProgress[index])
                        && hypothesisText.length() > hypothesisProgress[index]
                                .length()) {
                    hypothesisAccumulatedWeights[index] += propabilityWeight;
                    hypothesisProgress[index] = hypothesisText;
                    logger.info(
                            "'" + hypothesisText + "' + " + propabilityWeight);
                } else {
                    logger.info("Ignoring hypothesis '" + hypothesisText);
                }
            }

            private double propabilityWeight(SpeechRecognitionResult result) {
                return result.propability * wordCount(result.text);
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

    private Event<SpeechRecognitionImplementation, SpeechRecognizedEventArgs> recognitionRejected() {
        return new Event<SpeechRecognitionImplementation, SpeechRecognizedEventArgs>() {
            @Override
            public void run(SpeechRecognitionImplementation sender,
                    SpeechRecognizedEventArgs eventArgs) {
                if (!enabled)
                    return;
                // choose the choice with the highest hypothesis weight
                SpeechRecognitionHypothesisResult hypothesisResult = attemptToCreateHypothesisResult();
                if (hypothesisResult != null) {
                    if (hypothesisResult
                            .recognizedWith(recognitionConfidence)) {
                        consumeRejectedEventAndFireRecognizedEventInstead(
                                sender, eventArgs, hypothesisResult);
                    }
                } else {
                    logger.info("Speech recognition hypothesis dropped - "
                            + "multiple recognition results share the same accumulated weight - can't decide");
                }
            }

            private void consumeRejectedEventAndFireRecognizedEventInstead(
                    SpeechRecognitionImplementation sender,
                    SpeechRecognizedEventArgs eventArgs,
                    SpeechRecognitionHypothesisResult hypothesisResult) {
                eventArgs.consumed = true;
                SpeechRecognitionResult[] results = {
                        new SpeechRecognitionResult(hypothesisResult.index,
                                hypothesisResult.choice,
                                recognitionConfidence.propability,
                                recognitionConfidence) };
                SpeechRecognizedEventArgs recognitionCompletedEventArgs = new SpeechRecognizedEventArgs(
                        results);
                speechRecognizer.events.recognitionCompleted.run(sender,
                        recognitionCompletedEventArgs);
            }
        };
    }

    SpeechRecognitionHypothesisResult attemptToCreateHypothesisResult() {
        double maxValue = 0;
        int choiceWithMaxProbabilityIndex = 0;
        for (int i = 0; i < hypothesisAccumulatedWeights.length; i++) {
            double value = hypothesisAccumulatedWeights[i];
            SpeechRecognitionHypothesisEventHandler.logger.info("Result " + i
                    + ": '" + choices.get(i) + "' hypothesisCount=" + value);
            if (value > maxValue) {
                maxValue = value;
                choiceWithMaxProbabilityIndex = i;
            }
        }
        // sort out the case where two or more recognition results have the
        // same weight. This happens when they all start with the same text,
        // or (although unlikely) when the weighted sums add up to the same
        // value.
        if (numberOfHypothesisResults(maxValue) == 1) {
            return new SpeechRecognitionHypothesisResult(choices,
                    hypothesisProgress[choiceWithMaxProbabilityIndex], maxValue,
                    choiceWithMaxProbabilityIndex,
                    getHypothesisMinimumNumberOfWords(choices));
        } else {
            return null;
        }
    }

    private int numberOfHypothesisResults(double maxValue) {
        int n = 0;
        for (int i = 0; i < hypothesisAccumulatedWeights.length; i++) {
            if (hypothesisAccumulatedWeights[i] == maxValue) {
                n++;
            }
        }
        return n;
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
