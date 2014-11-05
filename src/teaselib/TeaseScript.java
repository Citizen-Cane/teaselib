package teaselib;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
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
	public final static String NoImage = "";
	public final static String MistressImage = null;

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
			// TODO Set voice as defined in script or config
			speechSynthesizer.setVoice(null);
		}
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
		if (name == MistressImage)
		{
			displayImage = MistressImage;
			// Render mistress image with the next message
		}
		else if (name == NoImage) {
			displayImage = NoImage;
			renderQueue.start(RenderNoImage.instance, teaseLib);
		}
		else
		{
			displayImage = name;
			renderQueue.start(new RenderImage(name), teaseLib);
		}
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
		return showChoices(choices, timeout > 0 ? () -> {
			teaseLib.host.sleep(timeout * 1000);
		} : null);
	}

	/**
	 * @param choices
	 * @param timeout
	 *            The timeout for the button set in seconds, or
	 *            TeaseScript.NoTimeout if no timeout is desired
	 * @param scriptFunction
	 * @return
	 */
	public int choose(List<String> choices, Runnable scriptFunction) {
		// To display buttons and to start scriptFunction at the same time,
		// completeAll() has to be called
		// in advance in order to finish all previous render commands,
		completeAll();
		int choice = showChoices(choices, scriptFunction);
		if (choice == Timeout) {
			renderQueue.completeAll();
		} else {
			renderQueue.endAll();
		}
		return choice;
	}

	public int showChoices(final List<String> choices, Runnable scriptFunction) {
		// arguments check
		for (String choice : choices) {
			if (choice == null) {
				throw new IllegalArgumentException("Choice may not be null");
			}
		}
		TeaseLib.log("choose: " + choices.toString());
		// Script closure
		FutureTask<Integer> scriptTask = scriptFunction == null ? null
				: new FutureTask<>(new Callable<Integer>() {
					@Override
					public Integer call() throws Exception {
						try {
							scriptFunction.run();
						} catch (ScriptInterruptedException e) {
							return null;
						}
						List<Delegate> clickables = teaseLib.host
								.getClickableChoices(choices);
						if (!clickables.isEmpty()) {
							Delegate clickable = clickables.get(0);
							if (clickable != null) {
								clickables.get(0).run();
							} else {
								// Means that the host implementation is
								// incomplete
								new IllegalStateException(
										"Host didn't return clickables for choices: "
												+ choices.toString());
							}
						}
						return new Integer(TeaseScript.Timeout);
					}
				});
		// Speech recognition
		List<Integer> srChoice = new ArrayList<>(1);
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
						// This assigns the result even if the buttons have
						// unrealized
						int choice = speechRecognitionResult.index;
						srChoice.add(choice);
						try {
							Delegate delegate = uiElements.get(choice);
							if (delegate != null) {
								delegate.run();
							} else {
								TeaseLib.log("Button gone for choice " + choice
										+ ": " + speechRecognitionResult.text);
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
		int choice;
		try {
			if (scriptTask != null) {
				ExecutorService executor = Executors.newFixedThreadPool(1);
				if (scriptTask != null) {
					executor.execute(scriptTask);
					// TODO Catch errors in script thread
				}
			}
			choice = teaseLib.host.choose(choices);
			if (scriptTask != null) {
				scriptTask.cancel(true);
			}
			// Wait to finish recognition
			final Lock speechRecognitionInProgress = SpeechRecognition.SpeechRecognitionInProgress;
			synchronized (speechRecognitionInProgress) {
				if (speechRecognitionInProgress.tryLock()) {
					speechRecognitionInProgress.unlock();
				} else {
					try {
						speechRecognitionInProgress.wait();
					} catch (InterruptedException e) {
						// Ignored
					}
				}
			}
		} finally {
			speechRecognizer.stopRecognition();
			speechRecognizer.events.recognitionCompleted
					.remove(speechRecognizedEvent);
		}
		// Assign result from speech recognition, tasks or button click
		if (!srChoice.isEmpty()) {
			choice = srChoice.get(0);
		} else {
			Integer r = null;
			// Script function completed?
			if (scriptTask != null) {
				if (!scriptTask.isCancelled()) {
					try {
						r = scriptTask.get();
					} catch (InterruptedException e) {
						TeaseLib.log(this, e);
					} catch (ExecutionException e) {
						Throwable cause = e.getCause();
						if (cause != null) {
							// Forward error from closure to main thread
							throw new RuntimeException(cause);
						} else {
							TeaseLib.log(this, e);
						}
					}
				}
			}
			if (r != null) {
				choice = r;
			} else {
				// User clicked button, choice already assigned
			}
		}
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
