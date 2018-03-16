package teaselib.core.speechrecognition;

import java.util.List;

public class SpeechRecognitionResult {
    /**
     * Defines the confidence the result was recognized with. Each confidence enum value comes with a predefined default
     * probability.
     *
     */
    public enum Confidence {
        Noise(0.0),
        Low(0.25),
        Normal(0.5),
        High(0.75);

        public static Confidence Default = Normal;

        /**
         * The default probability of the recognition confidence. The actual implementation may return a probability
         * value higher than the default, but still rate the confidence lower.
         */
        public final double probability;

        Confidence(double propability) {
            this.probability = propability;
        }

        public Confidence lower() {
            return lower(this);
        }

        public Confidence higher() {
            return higher(this);
        }

        public double reducedProbability() {
            return (probability + lower().probability) / 2.0;
        }

        public boolean isAsHighAs(Confidence confidence) {
            if (this.higherThan(confidence))
                return true;
            else
                return this == confidence;
        }

        public boolean isLowerThan(Confidence confidence) {
            return !isAsHighAs(confidence);
        }

        public boolean higherThan(Confidence confidence) {
            if (this == Normal && confidence == Low)
                return true;
            if (this == High && confidence == Low)
                return true;
            if (this == High && confidence == Normal)
                return true;
            return false;
        }

        public static Confidence lower(Confidence confidence) {
            if (confidence == High)
                return Normal;
            else if (confidence == Normal)
                return Low;
            else
                return confidence;
        }

        public static Confidence higher(Confidence confidence) {
            if (confidence == Low)
                return Normal;
            else if (confidence == Normal)
                return High;
            else
                return confidence;
        }

    }

    public static final int UNKNOWN_PHRASE_INDEX = -1;

    public final int index;
    public final String text;
    public final double probability;
    public final Confidence confidence;

    public SpeechRecognitionResult(int index, String text, double propability, Confidence confidence) {
        super();
        this.index = index;
        this.text = text;
        this.probability = propability;
        this.confidence = confidence;
    }

    public boolean isChoice(List<String> choices) {
        return UNKNOWN_PHRASE_INDEX < index && index < choices.size();
    }

    public boolean hasHigherProbabilityThan(SpeechRecognitionResult recognitionResult) {
        return probability > recognitionResult.probability
                || confidence.probability > recognitionResult.confidence.probability;
    }

    @Override
    public String toString() {
        return "#" + index + ": " + text + " (" + probability + ") -> confidence="
                + confidence.toString().toLowerCase();
    }
}
