package teaselib.speechrecognition;

import teaselib.core.events.Event;
import teaselib.core.events.EventSource;
import teaselib.speechrecognition.events.AudioLevelUpdatedEventArgs;
import teaselib.speechrecognition.events.AudioSignalProblemOccuredEventArgs;
import teaselib.speechrecognition.events.SpeechRecognitionStartedEventArgs;
import teaselib.speechrecognition.events.SpeechRecognizedEventArgs;

public class SpeechRecognitionEvents<S> {
    public final EventSource<S, SpeechRecognitionStartedEventArgs> recognitionStarted;

    public final EventSource<S, SpeechRecognizedEventArgs> speechDetected = new EventSource<S, SpeechRecognizedEventArgs>(
            "speechDetected");
    public final EventSource<S, SpeechRecognizedEventArgs> recognitionRejected;
    public final EventSource<S, SpeechRecognizedEventArgs> recognitionCompleted;

    public final EventSource<S, AudioLevelUpdatedEventArgs> audioLevelUpdated = new EventSource<S, AudioLevelUpdatedEventArgs>(
            "audioLevelUpdated");

    public final EventSource<S, AudioSignalProblemOccuredEventArgs> audioSignalProblemOccured = new EventSource<S, AudioSignalProblemOccuredEventArgs>(
            "audioSignalProblemOccured");

    public SpeechRecognitionEvents(
            Event<S, SpeechRecognitionStartedEventArgs> initial,
            Event<S, SpeechRecognizedEventArgs> completing) {
        recognitionStarted = new EventSource<S, SpeechRecognitionStartedEventArgs>(
                "recognitionStarted", initial, null);
        recognitionRejected = new EventSource<S, SpeechRecognizedEventArgs>(
                "recognitionRejected", null, completing);
        recognitionCompleted = new EventSource<S, SpeechRecognizedEventArgs>(
                "recognitionCompleted", null, completing);
    }
}
