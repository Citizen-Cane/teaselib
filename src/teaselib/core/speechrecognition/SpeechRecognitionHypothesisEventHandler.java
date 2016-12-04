package teaselib.core.speechrecognition;

import java.util.ArrayList;
import java.util.Arrays;
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
public class SpeechRecognitionHypothesisEventHandler {
    private static final Logger logger = LoggerFactory
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
    private final static double HypothesisMinimumAccumulatedWeight = 1.0;

    private final SpeechRecognition speechRecognizer;
    private final Event<SpeechRecognitionImplementation, SpeechRecognitionStartedEventArgs> recognitionStarted;
    private final Event<SpeechRecognitionImplementation, SpeechRecognizedEventArgs> speechDetected;
    private final Event<SpeechRecognitionImplementation, SpeechRecognizedEventArgs> recognitionRejected;

    private double[] hypothesisAccumulatedWeights;
    private String[] hypothesisProgress;

    private List<String> choices;
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

    class HypothesisResult {
        private final double maxValue;
        private final int choiceWithMaxProbabilityIndex;
        List<String> acceptedChoices = new ArrayList<String>();

        HypothesisResult(double[] hypothesisAccumulatedWeights) {
            double maxValue = 0;
            int choiceWithMaxProbabilityIndex = 0;
            for (int i = 0; i < hypothesisAccumulatedWeights.length; i++) {
                double value = hypothesisAccumulatedWeights[i];
                logger.info("Result " + i + ": '" + choices.get(i)
                        + "' hypothesisCount=" + value);
                if (value > maxValue) {
                    maxValue = value;
                    choiceWithMaxProbabilityIndex = i;
                }
            }
            this.maxValue = maxValue;
            this.choiceWithMaxProbabilityIndex = choiceWithMaxProbabilityIndex;
            // sort out the case where two or more recognition results have the
            // same weight. This happens when they all start with the same text,
            // or (although unlikely) when the weighted sums add up to the same
            // value.
            for (int i = 0; i < hypothesisAccumulatedWeights.length; i++) {
                if (hypothesisAccumulatedWeights[i] == maxValue) {
                    acceptedChoices.add(choices.get(i));
                }
            }
        }

        boolean recognizedAs(String choice, Confidence confidence) {
            List<Confidence> confidences = Arrays.asList(Confidence.High,
                    Confidence.Normal, Confidence.Low);
            int wordCount = wordCount(choice);
            for (Confidence desiredConfidence : confidences) {
                int confidenceBonus = confidences.indexOf(desiredConfidence);
                if (isRecognizedAs(
                        hypothesisProgress[choiceWithMaxProbabilityIndex],
                        desiredConfidence, confidenceBonus, wordCount)) {
                    return true;
                }
                if (desiredConfidence == confidence) {
                    // Don't test against a lower confidence than requested
                    break;
                }
            }
            return false;
        }

        private boolean isRecognizedAs(String hypothesisProgress,
                Confidence confidence,
                @SuppressWarnings("unused") int confidenceBonus,
                int wordCount) {
            int minimumNumberOfWordsForHypothesisRecognition = getHypothesisMinimumNumberOfWords(
                    acceptedChoices);
            // minimumNumberOfWordsForHypothesisRecognition -= confidenceBonus;
            // prompts with few words need a higher weight to be accepted
            double hypothesisAccumulatedWeight = wordCount >= minimumNumberOfWordsForHypothesisRecognition
                    ? HypothesisMinimumAccumulatedWeight
                    : HypothesisMinimumAccumulatedWeight
                            * (minimumNumberOfWordsForHypothesisRecognition
                                    - wordCount + 1);
            // TODO accumulated weight should be divided by the number of
            // hypothesized words
            // to get a value in [0...1] -> would be much clearer
            // -> commit first, then refactor
            boolean choiceWeightAccepted = maxValue >= hypothesisAccumulatedWeight
                    * confidence.propability;
            // Prompts with few words need more consistent speech detection
            // events
            // (doesn't alternate between different choices)
            int choiceHypothesisCount = wordCount(hypothesisProgress);
            boolean choiceDetectionCountAccepted = choiceHypothesisCount >= minimumNumberOfWordsForHypothesisRecognition;
            boolean choiceAccepted = choiceWeightAccepted
                    && choiceDetectionCountAccepted;
            if (!choiceWeightAccepted) {
                logger.info("Phrase '" + hypothesisProgress
                        + "' hypothesis accumulated weight=" + maxValue
                        + " < hypothesisAccumulatedWeight threshold="
                        + hypothesisAccumulatedWeight
                        + " is too low to accept hypothesis-based recognition for confidence "
                        + confidence.toString());
            }
            if (!choiceDetectionCountAccepted) {
                logger.info("Phrase '" + hypothesisProgress
                        + "' word detection count=" + choiceHypothesisCount
                        + " < threshold="
                        + minimumNumberOfWordsForHypothesisRecognition
                        + " is too low to accept hypothesis-based recognition for confidence "
                        + confidence.toString());
            }
            return choiceAccepted;
        }
    }

    private int getHypothesisMinimumNumberOfWords(List<String> choices) {
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
                HypothesisResult hypothesisResult = new HypothesisResult(
                        hypothesisAccumulatedWeights);
                final int size = hypothesisResult.acceptedChoices.size();
                if (size == 1) {
                    String choice = hypothesisResult.acceptedChoices.get(0);
                    if (hypothesisResult.recognizedAs(choice,
                            recognitionConfidence)) {
                        // Consume the recognitionRejected event and
                        // fire a RecognitionCompleted-event instead
                        eventArgs.consumed = true;
                        SpeechRecognitionResult[] results = {
                                new SpeechRecognitionResult(
                                        hypothesisResult.choiceWithMaxProbabilityIndex,
                                        choice,
                                        recognitionConfidence.propability,
                                        recognitionConfidence) };
                        SpeechRecognizedEventArgs recognitionCompletedEventArgs = new SpeechRecognizedEventArgs(
                                results);
                        speechRecognizer.events.recognitionCompleted.run(sender,
                                recognitionCompletedEventArgs);
                    }
                } else {
                    logger.info("Speech recognition hypothesis dropped - "
                            + size
                            + " recognition results share the same accumulated weight - can't decide");
                }
            }
        };
    }

    private static int wordCount(String text) {
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
