package teaselib.core.speechrecognition;

import teaselib.core.events.Event;
import teaselib.core.events.EventSource;
import teaselib.core.speechrecognition.events.AudioLevelUpdatedEventArgs;
import teaselib.core.speechrecognition.events.AudioSignalProblemOccuredEventArgs;
import teaselib.core.speechrecognition.events.SpeechRecognitionStartedEventArgs;
import teaselib.core.speechrecognition.events.SpeechRecognizedEventArgs;

public class SpeechRecognitionEvents<S> {
    public final EventSource<S, SpeechRecognitionStartedEventArgs> recognitionStarted;

    public final EventSource<S, SpeechRecognizedEventArgs> speechDetected = new EventSource<>("speechDetected");
    public final EventSource<S, SpeechRecognizedEventArgs> recognitionRejected;
    public final EventSource<S, SpeechRecognizedEventArgs> recognitionCompleted;

    public final EventSource<S, AudioLevelUpdatedEventArgs> audioLevelUpdated = new EventSource<>("audioLevelUpdated");

    public final EventSource<S, AudioSignalProblemOccuredEventArgs> audioSignalProblemOccured = new EventSource<>(
            "audioSignalProblemOccured");

    public SpeechRecognitionEvents(Event<S, SpeechRecognitionStartedEventArgs> initial,
            Event<S, SpeechRecognizedEventArgs> completing) {
        recognitionStarted = new EventSource<>("recognitionStarted", initial, null);
        recognitionRejected = new EventSource<>("recognitionRejected", null, completing);
        recognitionCompleted = new EventSource<>("recognitionCompleted", null, completing);
    }
}
