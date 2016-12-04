package teaselib.core.speechrecognition;

import java.util.Arrays;
import java.util.List;

import teaselib.core.speechrecognition.SpeechRecognitionResult.Confidence;

class HypothesisResult {
    final String choice;
    private final String hypothesisProgress;
    private final double maxValue;
    final int index;
    private int minimumNumberOfWords;

    HypothesisResult(List<String> choices, String hypothesisProgress,
            double probability, int index, int minimumNumberOfWords) {
        this.choice = choices.get(index);
        this.hypothesisProgress = hypothesisProgress;
        this.maxValue = probability;
        this.index = index;
        this.minimumNumberOfWords = minimumNumberOfWords;
    }

    boolean recognizedWith(Confidence confidence) {
        List<Confidence> confidences = Arrays.asList(Confidence.High,
                Confidence.Normal, Confidence.Low);
        int wordCount = SpeechRecognitionHypothesisEventHandler
                .wordCount(choice);
        for (Confidence desiredConfidence : confidences) {
            if (isRecognizedAs(hypothesisProgress, desiredConfidence,
                    wordCount)) {
                return true;
            } else if (desiredConfidence == confidence) {
                // Don't test against a lower confidence than requested
                break;
            }
        }
        return false;
    }

    private boolean isRecognizedAs(String hypothesisProgress,
            Confidence confidence, int wordCount) {
        // prompts with few words need a higher weight to be accepted
        double hypothesisAccumulatedWeight = wordCount >= minimumNumberOfWords
                ? SpeechRecognitionHypothesisEventHandler.HypothesisMinimumAccumulatedWeight
                : SpeechRecognitionHypothesisEventHandler.HypothesisMinimumAccumulatedWeight
                        * (minimumNumberOfWords - wordCount + 1);
        // TODO accumulated weight should be divided by the number of
        // hypothesized words
        // to get a value in [0...1] -> would be much clearer
        // -> commit first, then refactor
        boolean choiceWeightAccepted = maxValue >= hypothesisAccumulatedWeight
                * confidence.propability;
        // Prompts with few words need more consistent speech detection
        // events
        // (doesn't alternate between different choices)
        int choiceHypothesisCount = SpeechRecognitionHypothesisEventHandler
                .wordCount(hypothesisProgress);
        boolean choiceDetectionCountAccepted = choiceHypothesisCount >= minimumNumberOfWords;
        boolean choiceAccepted = choiceWeightAccepted
                && choiceDetectionCountAccepted;
        if (!choiceWeightAccepted) {
            SpeechRecognitionHypothesisEventHandler.logger.info("Phrase '"
                    + hypothesisProgress + "' hypothesis accumulated weight="
                    + maxValue + " < hypothesisAccumulatedWeight threshold="
                    + hypothesisAccumulatedWeight
                    + " is too low to accept hypothesis-based recognition for confidence "
                    + confidence.toString());
        }
        if (!choiceDetectionCountAccepted) {
            SpeechRecognitionHypothesisEventHandler.logger
                    .info("Phrase '" + hypothesisProgress
                            + "' word detection count=" + choiceHypothesisCount
                            + " < threshold=" + minimumNumberOfWords
                            + " is too low to accept hypothesis-based recognition for confidence "
                            + confidence.toString());
        }
        return choiceAccepted;
    }
}