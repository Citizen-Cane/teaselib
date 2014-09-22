package teaselib.texttospeech;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import teaselib.TeaseLib;
import teaselib.texttospeech.implementation.TeaseLibTTS;
import teaselib.util.Delegate;
import teaselib.util.DelegateThread;

public class TextToSpeech {

	private TextToSpeechImplementation tts;

	private DelegateThread delegateThread = new DelegateThread();

	private void ttsEngineNotInitialized()
	{
		throw new IllegalStateException("TTS engine not initialized");
	}

	public static final Lock AudioOutput = new ReentrantLock();
	
	public TextToSpeech() {
		try {
			Delegate delegate = new Delegate() {
				@Override
				public void run() {
					try {
						tts = new TeaseLibTTS();
					} catch (UnsatisfiedLinkError e) {
						TeaseLib.log(this, e);
					}
				}
			};
			delegateThread.run(delegate);
		} catch (Throwable t) {
			TeaseLib.log(this, t);
		}
	}

	/**
	 * @return Whether TextToSpeech is ready to render speech
	 */
	public boolean isReady() {
		return tts != null;
	}

	public Map<String, Voice> getVoices() {
		final Map<String, Voice> voices = new HashMap<>();
		if (tts != null)
		{
			Delegate delegate = new Delegate()  {
				@Override
				public void run() {
					tts.getVoices(voices);
				}
			};
			try {
				delegateThread.run(delegate);
			} catch (Throwable t) {
				TeaseLib.log(this, t);
			}
		}
		else
		{
			ttsEngineNotInitialized();
		}
		return voices;
	}

	/**
	 * Set a specific voice, or null for system default voice
	 * 
	 * @param voice
	 */
	public void setVoice(final Voice voice)
	{
		if (tts != null)
		{
			Delegate delegate = new Delegate() {
				@Override
				public void run() {
					tts.init(voice);
				}
			};
			try {
				delegateThread.run(delegate);
			} catch (Throwable t) {
				TeaseLib.log(this, t);
			}
		}
		else
		{
			ttsEngineNotInitialized();
		}
	}

	public void speak(final String prompt) {
		if (tts != null)
		{
			Delegate delegate = new Delegate() {
				@Override
				public void run() {
					synchronized(AudioOutput)
					{
						tts.speak(prompt);
					}
				}
			};
			try {
				delegateThread.run(delegate);
			} catch (Throwable t) {
				TeaseLib.log(this, t);
			}
		}
		else
		{
			ttsEngineNotInitialized();
		}
	}

	public void speak(final String prompt, final String wav) {
		if (tts != null)
		{
			Delegate delegate = new Delegate() {
				@Override
				public void run() {
					tts.speak(prompt, wav);
				}
			};
			try {
				delegateThread.run(delegate);
			} catch (Throwable t) {
				TeaseLib.log(this, t);
			}
		}
		else
		{
			ttsEngineNotInitialized();
		}
	}

//	public Delegates speechFinished() {
//		return tts.speechFinished;
//	}
	
	/**
	 * Estimate the duration for displaying the text when not spoken by speech synthesis.
	 * @param text Text to estimate the time needed to speak for
	 * @return duration, in milliseconds
	 */
	public static long getEstimatedSpeechDuration(String text)
	{
		long millisecondsPerLetter = 70;
		long pauseAfterParagraph = 1 * 1000;
		return text.length() * millisecondsPerLetter + pauseAfterParagraph;
	}

}
