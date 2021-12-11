package teaselib.core.speechrecognition;

/**
 * @author Citizen-Cane
 *
 */
public enum AudioSignalProblem {

    /**
     * No problem, all good.
     */
    None,

    /**
     * Too much background noise.
     */
    Noise,

    /**
     * Audio input not detected.
     * <p>
     * No signal at all, also observed with Microsoft RDP connections.
     */
    NoSignal,

    /**
     * Audio input too loud, waveform clipped.
     */
    TooLoud,

    /**
     * Audio input too quiet. Signal amplitude too low.
     */
    TooQuiet,

    /**
     * Audio input or speech is too fast, or too short. Nothing detected.
     */
    TooFast,

    /**
     * Audio input or speech is too slow, timeout. Nothing detected.
     */
    TooSlow

}
