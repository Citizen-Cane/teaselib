package teaselib.speechrecognition;

import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import teaselib.ScriptInterruptedException;
import teaselib.TeaseLib;
import teaselib.speechrecognition.events.SpeechRecognitionStartedEventArgs;
import teaselib.speechrecognition.events.SpeechRecognizedEventArgs;
import teaselib.speechrecognition.implementation.TeaseLibSR;
import teaselib.texttospeech.TextToSpeech;
import teaselib.util.Delegate;
import teaselib.util.DelegateThread;
import teaselib.util.Event;

public class SpeechRecognition {
	public final SpeechRecognitionEvents events = new SpeechRecognitionEvents();

	private String locale;
	
	private SpeechRecognitionImplementation sr;

	private DelegateThread delegateThread = new DelegateThread();

	private void recognizerNotInitialized() {
		throw new IllegalStateException("Recognizer not initialized");
	}

	public enum AudioSignalProblem {
		None, Noise, NoSignal, TooLoud, TooQuiet, TooFast, TooSlow
	}

	public static final Lock SpeechRecognitionInProgress = new ReentrantLock();

	public SpeechRecognition(String locale) {
		this.locale = locale.toLowerCase();
		try {
			Delegate delegate = new Delegate() {
				@Override
				public void run() {
					try {
						sr = new TeaseLibSR();
						sr.init(events, SpeechRecognition.this.locale);
					} catch (UnsatisfiedLinkError e) {
						TeaseLib.log(this, e);
					}
				}
			};
			delegateThread.run(delegate);
		} catch (Throwable t) {
			TeaseLib.log(this, t);
		}
		// Init further more
		if (sr != null) {
			// Allow other threads to wait while speech recognition is action
			Event<SpeechRecognitionImplementation, SpeechRecognitionStartedEventArgs> lockSpeechRecognitionInProgress = new Event<SpeechRecognitionImplementation, SpeechRecognitionStartedEventArgs>() {
				@Override
				public void run(SpeechRecognitionImplementation sender,
						SpeechRecognitionStartedEventArgs args) {
					SpeechRecognitionInProgress.lock();
				}
			};
			Event<SpeechRecognitionImplementation, SpeechRecognizedEventArgs> unlockSpeechRecognitionInProgress = new Event<SpeechRecognitionImplementation, SpeechRecognizedEventArgs>() {
				@Override
				public void run(SpeechRecognitionImplementation sender,
						SpeechRecognizedEventArgs args) {
					SpeechRecognitionInProgress.unlock();
				}
			};
			events.recognitionStarted.add(lockSpeechRecognitionInProgress);
			events.recognitionRejected.add(unlockSpeechRecognitionInProgress);
			events.recognitionCompleted.add(unlockSpeechRecognitionInProgress);
		}

	}

	/**
	 * @return Whether SpeechRecognition is ready to render speech
	 */
	public boolean isReady() {
		return sr != null;
	}

	public void startRecognition(final List<String> choices) {
		if (sr != null) {
			Delegate delegate = new Delegate() {
				@Override
				public void run() {
					sr.setChoices(choices);
					// Don't try to recognize speech during speech synthesis or
					// other speech related audio output
					synchronized (TextToSpeech.AudioOutput) {
						sr.startRecognition();
					}
				}
			};
			try {
				delegateThread.run(delegate);
			} catch (Throwable t) {
				TeaseLib.log(this, t);
			}
		} else {
			recognizerNotInitialized();
		}
	}

	public void stopRecognition() {
		if (sr != null) {
			Delegate delegate = new Delegate() {
				@Override
				public void run() {
					try {
						sr.stopRecognition();
					} finally {
						SpeechRecognitionInProgress.unlock();
					}
				}
			};
			try {
				delegateThread.run(delegate);
			} catch (Throwable t) {
				TeaseLib.log(this, t);
			}
		} else {
			recognizerNotInitialized();
		}
	}
	
	public void completeSpeechRecognitionInProgress()
	{
		try
		{
			SpeechRecognitionInProgress.lockInterruptibly();
		} catch (InterruptedException e) {
			throw new ScriptInterruptedException();
		}
		finally
		{
			SpeechRecognitionInProgress.unlock();
		}
	}
}