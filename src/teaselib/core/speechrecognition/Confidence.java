package teaselib.core.speechrecognition;

/**
 * @author Citizen-Cane
 *
 */
/**
 * Defines the confidence the result was recognized with. Each confidence enum value comes with a predefined default
 * probability.
 *
 */
public enum Confidence {
    Noise(0.0f),
    Low(0.25f),
    Normal(0.5f),
    High(0.75f),
    Definite(1.0f);

    public static final Confidence Default = Normal;

    /**
     * The default probability of the recognition confidence. The actual implementation may return a probability value
     * higher than the default, but still rate the confidence lower.
     */
    public final float probability;

    Confidence(float probability) {
        this.probability = probability;
    }

    public Confidence lower() {
        return lower(this);
    }

    public Confidence higher() {
        return higher(this);
    }

    public float slightlyRaisedProbability() {
        return probability + (higher().probability - probability) * 3.0f / 10.0f;
    }

    public float raisedProbability() {
        return probability + (higher().probability - probability) / 2.0f;
    }

    public float slightlyReducedProbability() {
        return probability + (lower().probability - probability) * 3.0f / 10.0f;
    }

    public float reducedProbability() {
        return probability + (lower().probability - probability) / 2.0f;
    }

    public static Confidence lower(Confidence confidence) {
        if (confidence == Definite)
            return High;
        else if (confidence == High)
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
        else if (confidence == High)
            return Definite;
        else
            return confidence;
    }

    public static Confidence valueOf(float probability) {
        if (probability >= Definite.probability)
            return High;
        else if (probability >= High.probability)
            return High;
        else if (probability >= Normal.probability)
            return Normal;
        else if (probability >= Low.probability)
            return Low;
        else
            return Noise;
    }

    public float weighted(int n) {
        return weighted(n, 2.0f);
    }

    public float weighted(int n, float k) {
        float start = slightlyRaisedProbability();
        float limit = slightlyReducedProbability();
        return weighted(n, k, start, limit);
    }

    public static float weighted(int n, float k, float start, float limit) {
        return limit + (start - limit) / (1 + (n - 1) / k);
    }

}
