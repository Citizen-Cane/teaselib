package teaselib.text;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import teaselib.Attitude;
import teaselib.ScriptInterruptedException;
import teaselib.TeaseLib;
import teaselib.TeaseScript;
import teaselib.audio.RenderSound;
import teaselib.image.ImageIterator;
import teaselib.image.RenderImage;
import teaselib.image.RenderNoImage;
import teaselib.texttospeech.TextToSpeech;
import teaselib.texttospeech.TextToSpeechPlayer;
import teaselib.userinterface.MediaRenderer;
import teaselib.userinterface.MediaRendererThread;

public class RenderMessage extends MediaRendererThread implements
		MediaRenderer, MediaRenderer.Threaded {

	private final Message message;
	private final TextToSpeechPlayer speechSynthesizer;
	private final ImageIterator imageIterator;
	private final Set<String> hints = new HashSet<>();

	private final static long DELAYBETWEENPARAGRAPHS = 500;
	private final static long DELAYATENDOFTEXT = 2000;

	public RenderMessage(Message message, TextToSpeechPlayer speechSynthesizer,
			ImageIterator imageIterator, Collection<String> hints) {
		if (message == null) {
			throw new NullPointerException();
		}
		this.message = message;
		this.speechSynthesizer = speechSynthesizer;
		this.imageIterator = imageIterator;
		hints.addAll(hints);
	}

	public Message getMessage() {
		return message;
	}

	@Override
	public void render() throws InterruptedException {
		Collection<String> paragraphs = message.getParagraphs();
		if (paragraphs.size() == 0) {
			teaseLib.host.show(null);
		} else {
			StringBuilder text = null;
			char ending = ' ';
			try {
				// Start speaking a message, replay prerecorded items or speak
				// with TTS later
				final Iterator<String> prerenderedSpeechItems;
				if (speechSynthesizer != null) {
					prerenderedSpeechItems = speechSynthesizer
							.selectVoice(message);
				} else {
					prerenderedSpeechItems = new ArrayList<String>().iterator();
				}
				// Process message paragraphs
				String image = null;
				for (Iterator<String> it = paragraphs.iterator(); it.hasNext();) {
					String line = it.next();
					Set<String> additionalHints = new HashSet<>();
					additionalHints.addAll(hints);
					// Handle message commands
					do {
						TeaseLib.log(line);
						String command = line.toLowerCase();
						if (Attitude.matches(command)) {
							additionalHints.add(command);
						} else if (command.endsWith(".png")
								|| command.endsWith(".jpg")) {
							image = line;
							new RenderImage(image).render(teaseLib);
						} else if (command.endsWith(".wav")
								|| command.endsWith(".ogg")
								|| command.endsWith(".mp3")) {
							new RenderSound(line).render(teaseLib);
						}
						// TODO Handle desktop item, but how would we detect
						// such strings (*.*) ?
						else if (line.equals(TeaseScript.DominantImage)) {
							image = null;
						} else if (line.equals(TeaseScript.NoImage)) {
							image = line;
							RenderNoImage.instance.render(teaseLib);
						} else {
							break;
						}
					} while (true);
					// Handle message text
					if (text == null) {
						text = new StringBuilder(line);
					} else if (ending == ',') {
						text.append(" ");
						text.append(line);
					} else {
						text.append("\n\n");
						text.append(line);
					}
					// Set image
					if (imageIterator != null && image == null) {
						String[] hintArray = new String[additionalHints.size()];
						hintArray = additionalHints.toArray(hintArray);
						imageIterator.hint(hintArray);
						teaseLib.host.setImage(imageIterator.next());
					}
					teaseLib.host.show(text.toString());
					// First message shown - start part completed
					notifyStartCompleted();
					final boolean lastParagraph = !it.hasNext();
					if (speechSynthesizer != null) {
						speechSynthesizer.speak(line, prerenderedSpeechItems,
								completedAll, teaseLib);
					} else {
						// Text is not meant to be spoken, just to be displayed
						// -> don't wait
					}
					if (endThread) {
						break;
					}
					if (lastParagraph) {
						// Interaction should start before the final delay
						finishedMandatoryParts = true;
						synchronized (completedMandatoryParts) {
							completedMandatoryParts.notifyAll();
						}
						synchronized (completedAll) {
							teaseLib.host.sleep(DELAYATENDOFTEXT);
						}
						endThread = true;
					} else {
						synchronized (completedAll) {
							teaseLib.host.sleep(DELAYBETWEENPARAGRAPHS);
						}
					}
					ending = line.isEmpty() ? ' ' : line
							.charAt(line.length() - 1);
					// TODO Nice, but in SexScripts text is always centered
					// vertically,
					// so the text kind of scrolls up when multiple paragraphs
					// are
					// displayed.
					// And because we don't know about the Word Wrap in
					// SexScripts,
					// there's no way to estimate the wrap or just insert
					// empty
					// lines.
					if (endThread) {
						break;
					}
				}
			} catch (ScriptInterruptedException e) {
				// Expected
			} catch (Throwable t) {
				TeaseLib.log(this, t);
				teaseLib.host.show(t.getMessage());
			}
		}
	}

	@Override
	public String toString() {
		long delay = 0;
		Collection<String> paragraphs = message.getParagraphs();
		for (Iterator<String> it = paragraphs.iterator(); it.hasNext();) {
			String paragraph = it.next();
			delay += TextToSpeech.getEstimatedSpeechDuration(paragraph);
			if (it.hasNext()) {
				delay += DELAYBETWEENPARAGRAPHS;
			} else {
				delay += DELAYATENDOFTEXT;
			}
		}
		return "Estimated delay = "
				+ String.format("%.2f", (double) delay / 1000);
	}

}
