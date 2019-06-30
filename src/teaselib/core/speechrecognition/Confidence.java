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
    High(0.75f);

    public static Confidence Default = Normal;

    /**
     * The default probability of the recognition confidence. The actual implementation may return a probability value
     * higher than the default, but still rate the confidence lower.
     */
    public final float probability;

    Confidence(float propability) {
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

    public static Confidence valueOf(float propability) {
        if (propability > High.probability)
            return High;
        if (propability > Normal.probability)
            return Normal;
        if (propability > Low.probability)
            return Low;
        return Noise;
    }
}
