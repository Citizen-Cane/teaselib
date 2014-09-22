package teaselib;

import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;

import teaselib.audio.RenderSound;
import teaselib.image.ImageIterator;
import teaselib.image.RenderImage;
import teaselib.image.RenderImageIterator;
import teaselib.image.RenderNoImage;
import teaselib.speechrecognition.SpeechRecognition;
import teaselib.speechrecognition.SpeechRecognitionImplementation;
import teaselib.speechrecognition.SpeechRecognitionResult;
import teaselib.speechrecognition.events.SpeechRecognizedEventArgs;
import teaselib.text.Message;
import teaselib.text.RenderDelay;
import teaselib.text.RenderMessage;
import teaselib.texttospeech.TextToSpeech;
import teaselib.texttospeech.Voice;
import teaselib.userinterface.MediaRenderer;
import teaselib.util.Delegate;
import teaselib.util.Event;
import teaselib.util.RenderDesktopItem;

public abstract class TeaseScript extends TeaseScriptBase {

	public static final int Yes = 0;
	public static final int No = 1;
	public static final int Timeout = -1;

	public static final int NoTimeout = 0;

	public ImageIterator mistress = null;
	private String displayImage = MistressImage;
	private final static String NoImage = "";
	private final static String MistressImage = null;

	public final TextToSpeech speechSynthesizer;
	public final SpeechRecognition speechRecognizer;

	public TeaseScript(TeaseLib teaseLib) {
		super(teaseLib);
		speechSynthesizer = new TextToSpeech();
		speechRecognizer = new SpeechRecognition();
		if (speechSynthesizer.isReady()) {
			Map<String, Voice> voices = speechSynthesizer.getVoices();
			for (Voice voice : voices.values()) {
				TeaseLib.log(voice.toString());
			}
			// TODO Set voice as defined in script
			speechSynthesizer.setVoice(null);
		}
	}

	public TeaseScript(TeaseScript rvalue)
	{
		super(rvalue.teaseLib);
		this.speechSynthesizer = rvalue.speechSynthesizer;
		this.speechRecognizer = rvalue.speechRecognizer;
	}

	/**
	 * Return a random number
	 * 
	 * @param min
	 * @param max
	 * @return A value in the interval [min, max]
	 */
	public int getRandom(int min, int max) {
		return teaseLib.host.getRandom(min, max);
	}

	public void setImage(String name) {
		final MediaRenderer image;
		if (name != null) {
			image = new RenderImage(name);
			displayImage = name;
		} else {
			image = RenderNoImage.instance;
			displayImage = NoImage;
		}
		renderQueue.start(image, teaseLib);
	}

	public void showDesktopItem(String name) {
		MediaRenderer desktopItem = new RenderDesktopItem(name);
		renderQueue.start(desktopItem, teaseLib);
	}

	public void playSound(String soundFile) {
		renderQueue.start(new RenderSound(soundFile), teaseLib);
	}

	public void say(String text) {
		Message message = new Message();
		if (text != null) {
			message.add(text);
		}
		say(message);
	}

	public void say(Message text) {
		RenderMessage renderMessage = new RenderMessage(text,
				speechSynthesizer, displayImage == MistressImage ? mistress
						: null);
		renderQueue.start(renderMessage, teaseLib);
		displayImage = MistressImage;
	}

	/**
	 * Show instructional text, this is not spoken, just displayed, and to be
	 * used for hints/cues alongside spoken dialog
	 * 
	 * @param text
	 *            The text top be displayed
	 */
	public void show(String text) {
		if (displayImage == MistressImage) {
			renderQueue.start(new RenderImageIterator(mistress), teaseLib);
		}
		RenderMessage renderMessage = new RenderMessage(new Message(text),
				null, null);
		renderQueue.start(renderMessage, teaseLib);
		displayImage = MistressImage;
	}

	/**
	 * Wait the requested numbers of seconds. The time starts to count from
	 * completing the last teaselib command, or from starting the last teaselib
	 * thread.
	 * 
	 * As a result, the wait would start right after a choice, or right after
	 * starting to display/speak text.
	 * 
	 * @param seconds
	 */
	public void delay(int seconds) {
		RenderDelay renderDelay = new RenderDelay(seconds);
		renderQueue.start(renderDelay, teaseLib);
	}
	
	/**
	 * Displays the requested choices in the user interface after the mandatory
	 * parts of all renderers have been completed. This means especially that
	 * all text has been displayed and spoken.
	 * 
	 * @param choices
	 * @return
	 */
	public int choose(List<String> choices) {
		return choose(choices, TeaseScript.NoTimeout);
	}

	/**
	 * Display choices. Won't wait for mandatory parts to complete.
	 * 
	 * @param choices
	 * @param timeout
	 *            The timeout for the button set in seconds, or 0 if no timeout
	 *            is desired
	 * @return The button index, or TeaseLib.None if the buttons timed out
	 */
	public int choose(final List<String> choices, int timeout) {
		completeMandatory();
		return showChoices(choices, timeout);
	}

	public int choose(List<String> choices, int timeout, Runnable scriptFunction)
	{
		// To display buttons and to start scriptFunction at the same time, completeAll() has to be called
		// in advance in order to finish all previous render commands,
		// TODO this breaks PCM "Stop" interaction, as that class misuses this to immediately display stop button
		completeAll();
		Thread scriptThread = new Thread(scriptFunction);
		scriptThread.start();
		int choice = showChoices(choices, timeout);
		if (choice == Timeout)
		{
			renderQueue.completeAll();
		}
		else
		{
			renderQueue.endAll();
		}
		try {
			scriptThread.join();
		} catch (InterruptedException e) {
			TeaseLib.log(this, e);
		}
		return choice;
	}

	public int showChoices(final List<String> choices, int timeout) {
		TeaseLib.log("choose: " + choices.toString());
		Event<SpeechRecognitionImplementation, SpeechRecognizedEventArgs> speechRecognizedEvent = new Event<SpeechRecognitionImplementation, SpeechRecognizedEventArgs>() {
			@Override
			public void run(SpeechRecognitionImplementation sender,
					SpeechRecognizedEventArgs eventArgs) {
				if (eventArgs.result.length == 1) {
					List<Delegate> uiElements = teaseLib.host
							.getClickableChoices(choices);
					// Click the button
					SpeechRecognitionResult speechRecognitionResult = eventArgs.result[0];
					if (speechRecognitionResult.isChoice(choices)) {
						int choice = speechRecognitionResult.index;
						try {
							Delegate delegate = uiElements.get(choice);
							if (delegate != null)
							{
								delegate.run();
							}
							else
							{
								TeaseLib.log("Button gone for choice " + choice + ": " + speechRecognitionResult.text);
							}
						} catch (Throwable t) {
							TeaseLib.log(this, t);
						}
					}
				} else {
					// TODO none or more than one result means incorrect
					// recognition
				}
			}
		}; 
		speechRecognizer.events.recognitionCompleted.add(speechRecognizedEvent);
		speechRecognizer.startRecognition(choices);
		// TODO Timeout doesn't wait for speech recognition in progress
		int choice = teaseLib.host.choose(choices, timeout);
		final Lock speechRecognitionInProgress = SpeechRecognition.SpeechRecognitionInProgress;
		synchronized(speechRecognitionInProgress)
		{
			if (speechRecognitionInProgress.tryLock())
			{
				speechRecognitionInProgress.unlock();
			}
			else
			{
				try {
					speechRecognitionInProgress.wait();
				} catch (InterruptedException e) {
					// Ignored
				}
			}
		}
		speechRecognizer.stopRecognition();
		speechRecognizer.events.recognitionCompleted.remove(speechRecognizedEvent);
		renderQueue.endAll();
		return choice;
	}

	/**
	 * Display an array of checkboxes to set or unset
	 * 
	 * @param caption
	 *            The caption of the checkbox area
	 * @param choices
	 *            The labels of the check boxes
	 * @param values
	 *            Indicates whether each item is set or unset by setting the
	 *            corresponding index to false or true.
	 * @return
	 */
	public List<Boolean> showCheckboxes(String caption, List<String> choices,
			List<Boolean> values) {
		return teaseLib.host.showCheckboxes(caption, choices, values);
	}

}
