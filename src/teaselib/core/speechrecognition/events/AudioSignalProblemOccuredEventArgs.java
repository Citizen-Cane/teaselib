package teaselib.core.speechrecognition.events;

import teaselib.core.speechrecognition.SpeechRecognition.AudioSignalProblem;

public class AudioSignalProblemOccuredEventArgs extends SpeechRecognitionEventArgs
{
	public final AudioSignalProblem problem;

	public AudioSignalProblemOccuredEventArgs(AudioSignalProblem problem) {
		this.problem = problem;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + " Problem = " + problem.toString();
	}
}
