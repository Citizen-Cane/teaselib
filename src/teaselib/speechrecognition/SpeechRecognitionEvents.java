package teaselib.speechrecognition;

import teaselib.speechrecognition.events.AudioLevelUpdatedEventArgs;
import teaselib.speechrecognition.events.AudioSignalProblemOccuredEventArgs;
import teaselib.speechrecognition.events.SpeechRecognitionStartedEventArgs;
import teaselib.speechrecognition.events.SpeechRecognizedEventArgs;
import teaselib.util.Event;
import teaselib.util.EventSource;

public class SpeechRecognitionEvents<S> {
	public final EventSource<S, SpeechRecognitionStartedEventArgs> recognitionStarted;

	public final EventSource<S, SpeechRecognizedEventArgs> speechDetected = new EventSource<>(
			"speechDetected");
	public final EventSource<S, SpeechRecognizedEventArgs> recognitionRejected;
	public final EventSource<S, SpeechRecognizedEventArgs> recognitionCompleted;

	public final EventSource<S, AudioLevelUpdatedEventArgs> audioLevelUpdated = new EventSource<>(
			"audioLevelUpdated");

	public final EventSource<S, AudioSignalProblemOccuredEventArgs> audioSignalProblemOccured = new EventSource<>(
			"audioSignalProblemOccured");

	public SpeechRecognitionEvents(Event<S, SpeechRecognitionStartedEventArgs> initial, Event<S, SpeechRecognizedEventArgs> completing) {
		recognitionStarted = new EventSource<>(
				"recognitionStarted", initial, null);
		recognitionRejected = new EventSource<>(
				"recognitionRejected", null, completing);
		recognitionCompleted = new EventSource<>(
				"recognitionCompleted", null, completing);
	}
}
