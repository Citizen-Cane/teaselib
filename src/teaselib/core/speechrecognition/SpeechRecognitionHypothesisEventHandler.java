/**
 * 
 */
package teaselib.core.speechrecognition;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import teaselib.TeaseLib;
import teaselib.core.events.Event;
import teaselib.core.speechrecognition.SpeechRecognitionResult.Confidence;
import teaselib.core.speechrecognition.events.SpeechRecognitionStartedEventArgs;
import teaselib.core.speechrecognition.events.SpeechRecognizedEventArgs;

/**
 * @author someone
 *
 */
public class SpeechRecognitionHypothesisEventHandler {
    /**
     * Hypothesis speech recognition is used for longer sentences, as short
     * sentences or single word recognitions are prone to error. In fact, for
     * single word phrases, the recognizer may recognize anything.
     */
    final static int HypothesisMinimumNumberOfWords = 3;

    /**
     * This adjusts the sensibility of the hypothesis rating. The better the
     * microphone, the higher this value should be. For a standard webcam, 1/2
     * seems to be a good start, however low values may lead to wrong
     * recognitions
     */
    final static double HypothesisMinimumAccumulatedWeight = 0.5;

    private final SpeechRecognition speechRecognizer;
    private final Event<SpeechRecognitionImplementation, SpeechRecognitionStartedEventArgs> recognitionStarted;
    private final Event<SpeechRecognitionImplementation, SpeechRecognizedEventArgs> speechDetected;
    private final Event<SpeechRecognitionImplementation, SpeechRecognizedEventArgs> recognitionRejected;

    private double[] hypothesisAccumulatedWeights;
    private String[] hypothesisProgress;

    private List<String> choices;
    private Confidence recognitionConfidence = Confidence.Default;

    public SpeechRecognitionHypothesisEventHandler(
            SpeechRecognition speechRecognizer) {
        super();
        this.speechRecognizer = speechRecognizer;
        this.recognitionStarted = recognitionStarted();
        this.speechDetected = speechDetected();
        this.recognitionRejected = recognitionRejected();
        speechRecognizer.events.recognitionStarted.add(recognitionStarted);
        speechRecognizer.events.speechDetected.add(speechDetected);
        speechRecognizer.events.recognitionRejected.add(recognitionRejected);
    }

    public void setChoices(List<String> choices) {
        this.choices = choices;
    }

    public void setConfidence(Confidence recognitionConfidence) {
        this.recognitionConfidence = recognitionConfidence;
    }

    private Event<SpeechRecognitionImplementation, SpeechRecognitionStartedEventArgs> recognitionStarted() {
        return new Event<SpeechRecognitionImplementation, SpeechRecognitionStartedEventArgs>() {
            @Override
            public void run(SpeechRecognitionImplementation sender,
                    SpeechRecognitionStartedEventArgs eventArgs) {
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
                // The Microsoft SAPI based SR is supposed to return
                // multiple results, however usually only one entry is
                // returned when hypothesizing a recognition result and
                // multiple choices start with the same words
                final SpeechRecognitionResult[] recognitionResults = eventArgs.result;
                if (recognitionResults.length == 1) {
                    // Manually search for all choices that start with the
                    // hypothesis, and add the probability weight for each
                    final SpeechRecognitionResult hypothesis = eventArgs.result[0];
                    String hypothesisText = hypothesis.text;
                    final double propabilityWeight = propabilityWeight(hypothesis);
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
                        final double propabilityWeight = propabilityWeight(hypothesis);
                        final int index = hypothesis.index;
                        updateHypothesisProgress(index, hypothesis.text,
                                propabilityWeight);
                    }
                }
            }

            private void updateHypothesisProgress(int index,
                    final String hypothesisText, double propabilityWeight) {
                // Only update if the hypothesis is progressing,
                // e.g. sort out detection duplicates
                if (hypothesisText.startsWith(hypothesisProgress[index])
                        && hypothesisText.length() > hypothesisProgress[index]
                                .length()) {
                    hypothesisAccumulatedWeights[index] += propabilityWeight;
                    hypothesisProgress[index] = hypothesisText;
                    TeaseLib.log("'" + hypothesisText + "' + "
                            + propabilityWeight);
                } else {
                    TeaseLib.log("Ignoring hypothesis '" + hypothesisText);
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
        final List<String> acceptedChoices = new ArrayList<String>();

        HypothesisResult(double[] hypothesisAccumulatedWeights) {
            double maxValue = 0;
            int choiceWithMaxProbabilityIndex = 0;
            for (int i = 0; i < hypothesisAccumulatedWeights.length; i++) {
                double value = hypothesisAccumulatedWeights[i];
                TeaseLib.log("Result " + i + ": '" + choices.get(i)
                        + "' hypothesisCount=" + value);
                if (value > maxValue) {
                    maxValue = value;
                    choiceWithMaxProbabilityIndex = i;
                }
            }
            this.maxValue = maxValue;
            this.choiceWithMaxProbabilityIndex = choiceWithMaxProbabilityIndex;
            // sort out the case where two or more recognition
            // results have the same weight.
            // This happens when they all start with the same text
            double weight = 0;
            for (int i = 0; i < hypothesisAccumulatedWeights.length; i++) {
                if (weight == maxValue) {
                    acceptedChoices.add(choices.get(i));
                }
            }
        }

        boolean recognizedAs(String choice, Confidence confidence) {
            final List<Confidence> confidences = Arrays.asList(Confidence.High,
                    Confidence.Normal, Confidence.Low);
            int wordCount = wordCount(choice);
            for (Confidence desiredConfidence : confidences) {
                int confidenceBonus = confidences.indexOf(desiredConfidence);
                if (isRecognizedAs(choice, desiredConfidence, confidenceBonus,
                        wordCount)) {
                    return true;
                }
                if (desiredConfidence == confidence) {
                    // Don't test against a lower confidence than requested
                    break;
                }
            }
            return false;
        }

        private boolean isRecognizedAs(String choice, Confidence confidence,
                int confidenceBonus, int wordCount) {
            int minimumNumberOfWordsForHypothesisRecognition = HypothesisMinimumNumberOfWords
                    - confidenceBonus;
            // prompts with few words need a higher weight to be
            // accepted
            double hypothesisAccumulatedWeight = wordCount >= minimumNumberOfWordsForHypothesisRecognition ? HypothesisMinimumAccumulatedWeight
                    : HypothesisMinimumAccumulatedWeight
                            * (minimumNumberOfWordsForHypothesisRecognition
                                    - wordCount + 1);
            boolean choiceWeightAccepted = maxValue >= hypothesisAccumulatedWeight
                    * confidence.propability;
            // Prompts with few words need more consistent speech
            // detection events (doesn't alternate between different
            // choices)
            int choiceHypothesisCount = wordCount(hypothesisProgress[choiceWithMaxProbabilityIndex]);
            boolean choiceDetectionCountAccepted = choiceHypothesisCount >= minimumNumberOfWordsForHypothesisRecognition
                    || choiceHypothesisCount >= minimumNumberOfWordsForHypothesisRecognition;
            boolean choiceAccepted = choiceWeightAccepted
                    && choiceDetectionCountAccepted;
            if (!choiceWeightAccepted) {
                TeaseLib.log("Phrase '"
                        + choice
                        + "' accumulated weight="
                        + maxValue
                        + " < "
                        + hypothesisAccumulatedWeight
                        + " is too low to accept hypothesis-based recognition for confidence "
                        + confidence.toString());
            }
            if (!choiceDetectionCountAccepted) {
                TeaseLib.log("Phrase '"
                        + choice
                        + "' detection count="
                        + choiceHypothesisCount
                        + " < "
                        + " is too low to accept hypothesis-based recognition for confidence "
                        + confidence.toString());
            }
            return choiceAccepted;
        }
    }

    private Event<SpeechRecognitionImplementation, SpeechRecognizedEventArgs> recognitionRejected() {
        return new Event<SpeechRecognitionImplementation, SpeechRecognizedEventArgs>() {
            @Override
            public void run(SpeechRecognitionImplementation sender,
                    SpeechRecognizedEventArgs eventArgs) {
                // choose the choice with the highest hypothesis weight
                HypothesisResult hypothesisResult = new HypothesisResult(
                        hypothesisAccumulatedWeights);
                if (hypothesisResult.acceptedChoices.size() == 1) {
                    String choice = hypothesisResult.acceptedChoices.get(0);
                    if (hypothesisResult.recognizedAs(choice,
                            recognitionConfidence)) {
                        // Consume the recognitionRejected event and
                        // fire a RecognitionCompleted-event instead
                        eventArgs.consumed = true;
                        SpeechRecognitionResult[] results = { new SpeechRecognitionResult(
                                hypothesisResult.choiceWithMaxProbabilityIndex,
                                choice, recognitionConfidence.propability,
                                recognitionConfidence) };
                        SpeechRecognizedEventArgs recognitionCompletedEventArgs = new SpeechRecognizedEventArgs(
                                results);
                        speechRecognizer.events.recognitionCompleted.run(
                                sender, recognitionCompletedEventArgs);
                    }
                } else {
                    TeaseLib.log("Speech recognition hypothesis dropped - several recognition results share the same accumulated weight - can't decide");
                }
            }
        };
    }

    private static int wordCount(String text) {
        String preparatedText = text;
        preparatedText = preparatedText.replace(",", " ");
        preparatedText = preparatedText.replace(".", " ");
        preparatedText = preparatedText.replace("!", " ");
        preparatedText.trim();
        return preparatedText.split(" ").length;
    }

    /**
     * Must be called in order to remove events.
     */
    public void dispose() {
        if (recognitionStarted != null)
            speechRecognizer.events.recognitionStarted
                    .remove(recognitionStarted);
        if (speechDetected != null)
            speechRecognizer.events.speechDetected.remove(speechDetected);
        if (recognitionRejected != null)
            speechRecognizer.events.recognitionRejected
                    .remove(recognitionRejected);
    }
}
