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
import teaselib.text.Message.Part;
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
                StringBuilder accumulatedText = null;
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
                for (Iterator<Part> it = message.iterator(); it.hasNext();) {
                    Set<String> additionalHints = new HashSet<String>();
                    additionalHints.addAll(hints);
                    // Handle message commands
                    Part part;
                    do {
                        part = it.next();
                        TeaseLib.log(part.type.toString() + ": " + part.value);
                        if (part.type == Message.Type.Image) {
                            displayImage = part.value;
                        } else if (part.type == Message.Type.Sound) {
                            new RenderSound(part.value).render(teaseLib);
                        } else if (part.type == Message.Type.DesktopItem) {
                            new RenderDesktopItem(part.value).render(teaseLib);
                        } else if (part.type == Message.Type.Mood) {
                            // Mood
                            additionalHints.add(part.value);
                        } else if (part.type == Message.Type.Keyword) {
                            String keyword = part.value;
                            if (keyword == TeaseScript.DominantImage) {
                                // Mistress image
                                displayImage = TeaseScript.DominantImage;
                            } else if (keyword == TeaseScript.NoImage) {
                                // No image
                                displayImage = TeaseScript.NoImage;
                            } else if (keyword == Message.ShowChoices) {
                                // Complete the mandatory part of the
                                // message
                                mandatoryCompleted();
                            } else {
                                // Unimplemented keyword
                                throw new IllegalArgumentException(
                                        "Unimplemented keyword: " + keyword);
                            }
                        } else if (part.type == Message.Type.Delay) {
                            // Pause
                            String[] cmd = part.value.split(" ");
                            if (cmd.length == 1) {
                                // Fixed pause
                                teaseLib.host.sleep(DELAYATENDOFTEXT);
                            } else if (cmd.length > 1) {
                                try {
                                    double delay = Double.parseDouble(cmd[1]) * 1000;
                                    teaseLib.host.sleep((int) delay);
                                } catch (NumberFormatException ignore) {
                                    // Fixed pause
                                    teaseLib.host.sleep(DELAYATENDOFTEXT);
                                }
                            }
                        } else {
                            // Text detected
                            break;
                        }
                    } while (true);
                    // Handle message text
                    if (accumulatedText == null) {
                        accumulatedText = new StringBuilder(part.value);
                    } else if (ending == ',') {
                        accumulatedText.append(" ");
                        accumulatedText.append(part.value);
                    } else {
                        accumulatedText.append("\n\n");
                        accumulatedText.append(part.value);
                    }
                    // Apply image and text
                    Image image;
                    try {
                        if (displayImage == TeaseScript.DominantImage) {
                            String[] hintArray = new String[additionalHints
                                    .size()];
                            hintArray = additionalHints.toArray(hintArray);
                            imageIterator.hint(hintArray);
                            image = imageIterator.next();
                        } else if (displayImage == TeaseScript.NoImage) {
                            image = null;
                        } else {
                            // TODO Cache image or detect reusage, since
                            // currently the same image is reloaded for each
                            // text part (usually when setting the image outside
                            // the message)
                            image = teaseLib.resources.image(IMAGES
                                    + displayImage);
                        }
                    } catch (Exception e) {
                        accumulatedText.append("\n" + e.getClass() + ": "
                                + e.getMessage() + "\n");
                        image = null;
                    }
                    teaseLib.host.show(image, accumulatedText.toString());
                    // First message shown - start part completed
                    startCompleted();
                    final boolean lastParagraph = !it.hasNext();
                    if (speechSynthesizer != null) {
                        speechSynthesizer.speak(part.value,
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
                    String value = part.value;
                    ending = value.isEmpty() ? ' ' : value.charAt(value
                            .length() - 1);
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
        Collection<Part> paragraphs = message.getParagraphs();
        for (Iterator<Part> it = paragraphs.iterator(); it.hasNext();) {
            Part paragraph = it.next();
            delay += TextToSpeech.getEstimatedSpeechDuration(paragraph.value);
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
