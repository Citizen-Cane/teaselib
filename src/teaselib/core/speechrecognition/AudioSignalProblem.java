package teaselib.core.speechrecognition;

public enum AudioSignalProblem {
    None,
    Noise,
    NoSignal,
    TooLoud,
    TooQuiet,
    TooFast,
    TooSlow
}