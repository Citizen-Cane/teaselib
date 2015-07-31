package teaselib.core.speechrecognition.events;


public class AudioLevelUpdatedEventArgs extends SpeechRecognitionEventArgs
{
	public final int audioLevel;

	public AudioLevelUpdatedEventArgs(int audioLevel) {
		this.audioLevel = audioLevel;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + " AudioLevel = " + Integer.toString(audioLevel);
	}
}
