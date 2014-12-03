package teaselib;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;

import teaselib.image.ImageIterator;
import teaselib.speechrecognition.SpeechRecognition;
import teaselib.speechrecognition.SpeechRecognitionImplementation;
import teaselib.speechrecognition.SpeechRecognitionResult;
import teaselib.speechrecognition.events.SpeechRecognizedEventArgs;
import teaselib.text.Message;
import teaselib.text.RenderMessage;
import teaselib.texttospeech.TextToSpeech;
import teaselib.texttospeech.TextToSpeechPlayer;
import teaselib.userinterface.MediaRenderer;
import teaselib.userinterface.MediaRendererQueue;
import teaselib.util.Delegate;
import teaselib.util.Event;

public abstract class TeaseScriptBase {

	public final TeaseLib teaseLib;

	protected final TextToSpeechPlayer speechSynthesizer;
	protected final SpeechRecognition speechRecognizer;

	protected final MediaRendererQueue renderQueue = new MediaRendererQueue();
	protected final Deque<MediaRenderer> deferredRenderers = new ArrayDeque<MediaRenderer>();

	public TeaseScriptBase(TeaseLib teaseLib, String locale) {
		this.teaseLib = teaseLib;
		speechSynthesizer = new TextToSpeechPlayer(teaseLib.resources,
				new TextToSpeech());
		speechRecognizer = new SpeechRecognition(locale);
	}

	/**
	 * Just wait for everything to be rendered (messages displayed, sounds
	 * played, delay expired), and continue execution of the script. This won't
	 * display a button, it just waits.
	 */
	public void completeAll() {
		renderQueue.completeAll();
	}

	/**
	 * Workaround as of now because PCMPlayer must display the stop button
	 * immediately
	 */
	public void completeMandatory() {
		renderQueue.completeMandatories();
	}

	public void renderMessage(Message message,
			TextToSpeechPlayer speechSynthesizer, ImageIterator dominantImages,
			String attitude) {
		completeAll();
		renderQueue.start(deferredRenderers, teaseLib);
		deferredRenderers.clear();
		Set<String> hints = new HashSet<>();
		// Within messages, images might change fast, and changing
		// the camera position, image size or aspect would be too distracting
		hints.add(ImageIterator.SameCameraPosition);
		hints.add(ImageIterator.SameResolution);
		hints.add(attitude);
		RenderMessage renderMessage = new RenderMessage(message,
				speechSynthesizer, dominantImages, hints);
		renderQueue.start(renderMessage, teaseLib);
	}

	public int showChoices(final List<String> choices, Runnable scriptFunction) {
		// arguments check
		for (String choice : choices) {
			if (choice == null) {
				throw new IllegalArgumentException("Choice may not be null");
			}
		}
		TeaseLib.log("choose: " + choices.toString());
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
		int choice;
		try {
			if (scriptTask != null) {
				ExecutorService executor = Executors.newFixedThreadPool(1);
				executor.execute(scriptTask);
				renderQueue.completeStarts();
				// TODO completeStarts() doesn't work because first we need to
				// wait for render threads that can be completed
				// Workaround: A bit unsatisfying, but otherwise the choice
				// buttons would appear too early
				teaseLib.host.sleep(300);
			}
			choice = teaseLib.host.choose(choices);
			if (scriptTask != null) {
				scriptTask.cancel(true);
			}
			speechRecognizer.completeSpeechRecognitionInProgress();
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
						// Wait for script task completion
						r = scriptTask.get();
					} catch (InterruptedException e) {
						throw new ScriptInterruptedException();
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
}
