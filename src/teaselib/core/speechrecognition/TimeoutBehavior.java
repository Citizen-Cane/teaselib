package teaselib.core.speechrecognition;

/**
 * How to handle speech recognition and timeout in script functions.
 *
 */
public enum TimeoutBehavior {
    /**
     * Cut the slave short: Ignore speech recognition and just finish.
     */
    InDubioContraReum("In Dubio Contra Reum"),

    /**
     * Be indulgent and let the user finish speaking before deciding about timeout. A recognized prompt will cancel
     * the timeout, even if time is up, and return the recognized choice instead of a timeout.
     */
    InDubioProDuriore("In Dubio Pro Duriore"),

    /**
     * Give the benefit of the doubt and stop the timeout on the first attempt to answer via speech recognition,
     * even if that recognition result will be rejected.
     * 
     * The prompt has of course still to be answered.
     */
    InDubioMitius("In Dubio Mitius");

    private final String displayName;

    TimeoutBehavior(String displayName) {
        this.displayName = displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}