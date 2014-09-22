package teaselib.speechrecognition;

import teaselib.speechrecognition.events.AudioLevelUpdatedEventArgs;
import teaselib.speechrecognition.events.AudioSignalProblemOccuredEventArgs;
import teaselib.speechrecognition.events.SpeechRecognitionStartedEventArgs;
import teaselib.speechrecognition.events.SpeechRecognizedEventArgs;
import teaselib.util.EventSource;

public class SpeechRecognitionEvents {
	public final EventSource<SpeechRecognitionImplementation, SpeechRecognitionStartedEventArgs> recognitionStarted = new EventSource<>("recognitionStarted");

	public final EventSource<SpeechRecognitionImplementation, SpeechRecognizedEventArgs> speechDetected = new EventSource<>("speechDetected");
	public final EventSource<SpeechRecognitionImplementation, SpeechRecognizedEventArgs> recognitionRejected = new EventSource<>("recognitionRejected");
	public final EventSource<SpeechRecognitionImplementation, SpeechRecognizedEventArgs> recognitionCompleted = new EventSource<>("recognitionCompleted");

	public final EventSource<SpeechRecognitionImplementation, AudioLevelUpdatedEventArgs> audioLevelUpdated = new EventSource<>("audioLevelUpdated");
	
	public final EventSource<SpeechRecognitionImplementation, AudioSignalProblemOccuredEventArgs> audioSignalProblemOccured = new EventSource<>("audioSignalProblemOccured");
}
