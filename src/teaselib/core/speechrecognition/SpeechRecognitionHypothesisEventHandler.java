/**
 * 
 */
package teaselib.core.speechrecognition;

import java.util.ArrayList;
import java.util.List;

import teaselib.TeaseLib;
import teaselib.core.ScriptFutureTask;
import teaselib.core.events.Delegate;
import teaselib.core.events.Event;
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

    public final TeaseLib teaseLib;
    public ScriptFutureTask scriptTask = null;

    private final SpeechRecognition speechRecognizer;
    private final Event<SpeechRecognitionImplementation, SpeechRecognitionStartedEventArgs> recognitionStarted;
    private final Event<SpeechRecognitionImplementation, SpeechRecognizedEventArgs> speechDetected;
    private final Event<SpeechRecognitionImplementation, SpeechRecognizedEventArgs> recognitionRejected;
    private final Event<SpeechRecognitionImplementation, SpeechRecognizedEventArgs> recognitionCompleted;

    private double[] hypothesisAccumulatedWeights;
    private String[] hypothesisProgress;

    List<String> derivedChoices;
    final List<Integer> srChoiceIndices;

    public SpeechRecognitionHypothesisEventHandler(TeaseLib teaseLib,
            SpeechRecognition speechRecognizer) {
        super();
        this.teaseLib = teaseLib;
        this.speechRecognizer = speechRecognizer;
        this.recognitionStarted = recognitionStarted();
        this.speechDetected = speechDetected();
        this.recognitionRejected = recognitionRejected();
        this.recognitionCompleted = recognitionCompleted();
        speechRecognizer.events.recognitionStarted.add(recognitionStarted);
        speechRecognizer.events.speechDetected.add(speechDetected);
        speechRecognizer.events.recognitionRejected.add(recognitionRejected);
        speechRecognizer.events.recognitionCompleted.add(recognitionCompleted);
        this.srChoiceIndices = new ArrayList<Integer>(1);
    }

    public void setChoices(List<String> derivedChoices) {
        this.derivedChoices = derivedChoices;
        srChoiceIndices.clear();
    }

    public int getChoiceIndex() {
        if (srChoiceIndices.isEmpty()) {
            return -1;
        } else {
            return srChoiceIndices.get(0);
        }
    }

    private Event<SpeechRecognitionImplementation, SpeechRecognitionStartedEventArgs> recognitionStarted() {
        return new Event<SpeechRecognitionImplementation, SpeechRecognitionStartedEventArgs>() {
            @Override
            public void run(SpeechRecognitionImplementation sender,
                    SpeechRecognitionStartedEventArgs eventArgs) {
                final int size = derivedChoices.size();
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
                    for (int index = 0; index < derivedChoices.size(); index++) {
                        String choice = derivedChoices.get(index).toLowerCase();
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

    private Event<SpeechRecognitionImplementation, SpeechRecognizedEventArgs> recognitionRejected() {
        return new Event<SpeechRecognitionImplementation, SpeechRecognizedEventArgs>() {
            @Override
            public void run(SpeechRecognitionImplementation sender,
                    SpeechRecognizedEventArgs eventArgs) {
                // choose the choice with the highest hypothesis weight
                double maxValue = 0;
                int maxChoiceIndex = 0;
                for (int i = 0; i < hypothesisAccumulatedWeights.length; i++) {
                    double value = hypothesisAccumulatedWeights[i];
                    TeaseLib.log("Result " + i + ": '" + derivedChoices.get(i)
                            + "' hypothesisCount=" + value);
                    if (value > maxValue) {
                        maxValue = value;
                        maxChoiceIndex = i;
                    }
                }
                // sort out the case where two or more recognition
                // results have the same weight.
                // This happens when they all start with the same text
                int numberOfCandidates = 0;
                for (double weight : hypothesisAccumulatedWeights) {
                    if (weight == maxValue) {
                        numberOfCandidates++;
                    }
                }
                if (numberOfCandidates == 1) {
                    final String choice = derivedChoices.get(maxChoiceIndex);
                    int wordCount = wordCount(choice);
                    // prompts with few words need a higher weight to be
                    // accepted
                    double hypothesisAccumulatedWeight = wordCount >= HypothesisMinimumNumberOfWords ? HypothesisMinimumAccumulatedWeight
                            : HypothesisMinimumAccumulatedWeight
                                    * (HypothesisMinimumNumberOfWords
                                            - wordCount + 1);
                    boolean choiceWeightAccepted = maxValue >= hypothesisAccumulatedWeight;
                    int choiceHypothesisCount = wordCount(hypothesisProgress[maxChoiceIndex]);
                    // Prompts with few words need more consistent speech
                    // detection events (doesn't alternate between different
                    // choices)
                    boolean choiceDetectionCountAccepted = choiceHypothesisCount >= HypothesisMinimumNumberOfWords
                            || choiceHypothesisCount >= wordCount;
                    if (choiceWeightAccepted && choiceDetectionCountAccepted) {
                        clickChoiceElement(derivedChoices, maxChoiceIndex,
                                choice);
                    }
                    if (!choiceWeightAccepted) {
                        TeaseLib.log("Phrase '"
                                + choice
                                + "' accumulated weight="
                                + maxValue
                                + " < "
                                + hypothesisAccumulatedWeight
                                + " is too low to accept hypothesis-based recognition");
                    }
                    if (!choiceDetectionCountAccepted) {
                        TeaseLib.log("Phrase '"
                                + choice
                                + "' detection count="
                                + choiceHypothesisCount
                                + " < "
                                + " is too low to accept hypothesis-based recognition");
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

    private Event<SpeechRecognitionImplementation, SpeechRecognizedEventArgs> recognitionCompleted() {
        return new Event<SpeechRecognitionImplementation, SpeechRecognizedEventArgs>() {
            @Override
            public void run(SpeechRecognitionImplementation sender,
                    SpeechRecognizedEventArgs eventArgs) {
                if (eventArgs.result.length == 1) {
                    // Find the button to click
                    SpeechRecognitionResult speechRecognitionResult = eventArgs.result[0];
                    if (!speechRecognitionResult.isChoice(derivedChoices)) {
                        throw new IllegalArgumentException(
                                speechRecognitionResult.toString());
                    }
                    clickChoiceElement(derivedChoices,
                            speechRecognitionResult.index,
                            speechRecognitionResult.text);
                } else {
                    // none or more than one result means incorrect
                    // recognition
                }
            }

        };
    }

    private void clickChoiceElement(final List<String> derivedChoices,
            int choice, String text) {
        // Assign the result even if the buttons have been unrealized
        srChoiceIndices.add(choice);
        List<Delegate> uiElements = teaseLib.host
                .getClickableChoices(derivedChoices);
        try {
            Delegate delegate = uiElements.get(choice);
            if (delegate != null) {
                if (scriptTask != null) {
                    scriptTask.cancel(true);
                }
                // Click the button
                delegate.run();
                TeaseLib.log("Clicked delegate for '" + text + "' index="
                        + choice);
            } else {
                TeaseLib.log("Button gone for choice " + choice + ": " + text);
            }
        } catch (Throwable t) {
            TeaseLib.log(this, t);
        }
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
        if (recognitionCompleted != null)
            speechRecognizer.events.recognitionCompleted
                    .remove(recognitionCompleted);
    }
}
