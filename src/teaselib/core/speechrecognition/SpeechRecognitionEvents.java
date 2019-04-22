package teaselib.core.speechrecognition;

import teaselib.core.events.Event;
import teaselib.core.events.EventSource;
import teaselib.core.speechrecognition.events.AudioLevelUpdatedEventArgs;
import teaselib.core.speechrecognition.events.AudioSignalProblemOccuredEventArgs;
import teaselib.core.speechrecognition.events.SpeechRecognitionStartedEventArgs;
import teaselib.core.speechrecognition.events.SpeechRecognizedEventArgs;

public class SpeechRecognitionEvents {
    public final EventSource<SpeechRecognitionStartedEventArgs> recognitionStarted;

    public final EventSource<SpeechRecognizedEventArgs> speechDetected = new EventSource<>("speechDetected");
    public final EventSource<SpeechRecognizedEventArgs> recognitionRejected;
    public final EventSource<SpeechRecognizedEventArgs> recognitionCompleted;

    public final EventSource<AudioLevelUpdatedEventArgs> audioLevelUpdated = new EventSource<>("audioLevelUpdated");

    public final EventSource<AudioSignalProblemOccuredEventArgs> audioSignalProblemOccured = new EventSource<>(
            "audioSignalProblemOccured");

    public SpeechRecognitionEvents(Event<SpeechRecognitionStartedEventArgs> initial,
            Event<SpeechRecognizedEventArgs> completing) {
        recognitionStarted = new EventSource<>("recognitionStarted", initial, null);
        recognitionRejected = new EventSource<>("recognitionRejected", null, completing);
        recognitionCompleted = new EventSource<>("recognitionCompleted", null, completing);
    }
}
