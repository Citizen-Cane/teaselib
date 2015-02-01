package teaselib.text;

import java.awt.Image;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import teaselib.ScriptInterruptedException;
import teaselib.TeaseLib;
import teaselib.TeaseScript;
import teaselib.audio.RenderSound;
import teaselib.image.ImageIterator;
import teaselib.texttospeech.TextToSpeech;
import teaselib.texttospeech.TextToSpeechPlayer;
import teaselib.userinterface.MediaRenderer;
import teaselib.userinterface.MediaRendererThread;
import teaselib.util.RenderDesktopItem;

public class RenderMessage extends MediaRendererThread implements
        MediaRenderer, MediaRenderer.Threaded {

    private final Message message;
    private final TextToSpeechPlayer speechSynthesizer;
    private final ImageIterator imageIterator;
    private String displayImage;
    private final Set<String> hints = new HashSet<String>();

    private final static long DELAYBETWEENPARAGRAPHS = 500;
    private final static long DELAYATENDOFTEXT = 2000;

    private static final String IMAGES = "images/";

    public RenderMessage(Message message, TextToSpeechPlayer speechSynthesizer,
            ImageIterator imageIterator, String displayImage,
            Collection<String> hints) {
        if (message == null) {
            throw new NullPointerException();
        }
        this.message = message;
        this.speechSynthesizer = speechSynthesizer;
        this.imageIterator = imageIterator;
        this.displayImage = displayImage;
        hints.addAll(hints);
    }

    public Message getMessage() {
        return message;
    }

    @Override
    public void render() throws InterruptedException {
        try {
            if (message.isEmpty()) {
                // Set image and no text
                if (displayImage == TeaseScript.NoImage) {
                    teaseLib.host.show(null, null);
                } else if (displayImage != TeaseScript.DominantImage) {
                    teaseLib.host.show(imageIterator.next(), null);
                } else {
                    String[] hintArray = new String[hints.size()];
                    hintArray = hints.toArray(hintArray);
                    imageIterator.hint(hintArray);
                    teaseLib.host.show(imageIterator.next(), null);
                }
            } else {
                StringBuilder text = null;
                char ending = ' ';
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
                for (Iterator<String> it = message.iterator(); it.hasNext();) {
                    Set<String> additionalHints = new HashSet<String>();
                    additionalHints.addAll(hints);
                    // Handle message commands
                    String paragraph = it.next();
                    do {
                        TeaseLib.log(paragraph);
                        if (Message.isFile(paragraph)) {
                            String fileOrKeyword = paragraph.toLowerCase();
                            if (Message.isImage(fileOrKeyword)) {
                                displayImage = paragraph;
                            } else if (Message.isSound(fileOrKeyword)) {
                                new RenderSound(paragraph).render(teaseLib);
                            } else {
                                new RenderDesktopItem(paragraph)
                                        .render(teaseLib);
                            }
                        } else {
                            if (Message.isMood(paragraph)) {
                                // Mood
                                additionalHints.add(paragraph);
                            } else {
                                String keyWord = paragraph.toLowerCase();
                                if (keyWord
                                        .equalsIgnoreCase(TeaseScript.DominantImage)) {
                                    // Mistress image
                                    displayImage = TeaseScript.DominantImage;
                                } else if (keyWord
                                        .equalsIgnoreCase(TeaseScript.NoImage)) {
                                    // No image
                                    displayImage = TeaseScript.NoImage;
                                } else if (keyWord
                                        .equalsIgnoreCase(Message.MandatoryCompleted)) {
                                    // Complete the mandatory part of the
                                    // message
                                    mandatoryCompleted();
                                } else if (keyWord.startsWith(Message.Delay)) {
                                    // Pause
                                    String[] cmd = paragraph.split(" ");
                                    if (cmd.length == 1) {
                                        // Fixed pause
                                        teaseLib.host.sleep(DELAYATENDOFTEXT);
                                    } else if (cmd.length > 1) {
                                        try {
                                            double delay = Double
                                                    .parseDouble(cmd[1]) * 1000;
                                            teaseLib.host.sleep((int) delay);
                                        } catch (NumberFormatException ignore) {
                                            // Fixed pause
                                            teaseLib.host
                                                    .sleep(DELAYATENDOFTEXT);
                                        }
                                    }
                                } else {
                                    // Text detected
                                    break;
                                }
                            }
                        }
                        // Advance after processing mood, file or keyword
                        paragraph = it.next();
                    } while (true);
                    // Handle message text
                    if (text == null) {
                        text = new StringBuilder(paragraph);
                    } else if (ending == ',') {
                        text.append(" ");
                        text.append(paragraph);
                    } else {
                        text.append("\n\n");
                        text.append(paragraph);
                    }
                    // Apply image and text
                    final Image image;
                    if (displayImage == TeaseScript.DominantImage) {
                        String[] hintArray = new String[additionalHints.size()];
                        hintArray = additionalHints.toArray(hintArray);
                        imageIterator.hint(hintArray);
                        image = imageIterator.next();
                    } else if (displayImage == TeaseScript.NoImage) {
                        // RenderNoImage.instance.render(teaseLib);
                        image = null;
                    } else {
                        // new RenderImage(image).render(teaseLib);
                        image = teaseLib.resources.image(IMAGES + displayImage);
                    }
                    teaseLib.host.show(image, text.toString());
                    // First message shown - start part completed
                    startCompleted();
                    final boolean lastParagraph = !it.hasNext();
                    if (speechSynthesizer != null) {
                        speechSynthesizer.speak(paragraph,
                                prerenderedSpeechItems, teaseLib);
                    } else {
                        // Text is not meant to be spoken, just to be displayed
                        // -> don't wait
                    }
                    if (endThread) {
                        break;
                    }
                    if (lastParagraph) {
                        // Interaction should start before the final delay
                        mandatoryCompleted();
                        teaseLib.host.sleep(DELAYATENDOFTEXT);
                        allCompleted();
                        endThread = true;
                    } else {
                        teaseLib.host.sleep(DELAYBETWEENPARAGRAPHS);
                    }
                    ending = paragraph.isEmpty() ? ' ' : paragraph
                            .charAt(paragraph.length() - 1);
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
            }
        } catch (ScriptInterruptedException e) {
            // Expected
        } catch (Throwable t) {
            TeaseLib.log(this, t);
            teaseLib.host.show(null, t.getMessage());
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

    @Override
    public void end() {
        // Cancel ongoing TTS speech
        if (speechSynthesizer != null) {
            speechSynthesizer.stop();
        }
        // Cancel prerecorded TTS speech
        // as well as any other sounds
        teaseLib.host.stopSounds();
        // TODO Only stop speech and sounds that
        // have been stated by this message renderer
        super.end();
    }
}
