package teaselib.text;

import java.util.Collection;
import java.util.Iterator;

import teaselib.ScriptInterruptedException;
import teaselib.TeaseLib;
import teaselib.image.ImageIterator;
import teaselib.texttospeech.TextToSpeech;
import teaselib.userinterface.MediaRenderer;
import teaselib.userinterface.MediaRendererThread;

public class RenderMessage extends MediaRendererThread implements MediaRenderer,
		MediaRenderer.Threaded {

	private final Message message;
	private final TextToSpeech speechSynthesizer;
	private final ImageIterator imageIterator;
	
	private final static long DELAYBETWEENPARAGRAPHS = 500;
	private final static long DELAYATENDOFTEXT = 2000;

	public RenderMessage(Message message, TextToSpeech speechSynthesizer, ImageIterator imageIterator) {
		if (message == null)
		{
			throw new NullPointerException();
		}
		this.message = message;
		this.speechSynthesizer = speechSynthesizer;
		this.imageIterator = imageIterator;
	}

	@Override
	public void render() throws InterruptedException {
		Collection<String> paragraphs = message.getParagraphs();
		if (paragraphs.size() == 0)
		{
			teaseLib.host.show(null);
		}
		else
		{
			StringBuilder text = null;
			char ending = ' ';
			try {
				for (Iterator<String> it = paragraphs.iterator(); it.hasNext();) {
					String paragraph = it.next();
					TeaseLib.log(paragraph);
					if (text == null) {
						text = new StringBuilder(paragraph);
					} else if (ending == ',') {
						text.append(" ");
						text.append(paragraph);
					} else {
						text.append("\n\n");
						text.append(paragraph);
					}
					if (imageIterator != null)
					{
						teaseLib.host.setImage(imageIterator.next());
					}
					teaseLib.host.show(text.toString());
					final boolean lastParagraph = !it.hasNext();
					// It's rude to interrupt mistress while she speaks,
					// so let's just render speech synchronous for now,
					// and decide about async speech later
					if (speechSynthesizer == null)
					{
						// Text is not meant to be spoken, just to be displayed -> don't wait
					}
					else if (!speechSynthesizer.isReady()) {
						// Unable to speak, just display the estimated duration
						long duration = TextToSpeech.getEstimatedSpeechDuration(paragraph);
						synchronized (completedAll) {
							teaseLib.host.sleep(duration);
						}
					} else {
						// Fully able to speak, wait until finished speaking
						try {
							speechSynthesizer.speak(paragraph);
						} catch (Throwable t) {
							TeaseLib.log(this, t);
							long duration = TextToSpeech.getEstimatedSpeechDuration(paragraph);
							synchronized (completedAll) {
								teaseLib.host.sleep(duration);
							}
						}
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
					ending = paragraph.charAt(paragraph.length() - 1);
					// TODO Nice, but in SexScripts text is always centered
					// vertically,
					// so the text kind of scrolls up when multiple paragraphs are
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
			} catch(ScriptInterruptedException e){
				// Expected
			} catch (Throwable t) {
				TeaseLib.log(this, t);
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
