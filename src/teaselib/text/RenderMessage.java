package teaselib.text;

import java.awt.Image;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import teaselib.Mood;
import teaselib.ScriptInterruptedException;
import teaselib.TeaseLib;
import teaselib.TeaseScript;
import teaselib.audio.RenderSound;
import teaselib.image.Images;
import teaselib.text.Message.Part;
import teaselib.texttospeech.TextToSpeech;
import teaselib.texttospeech.TextToSpeechPlayer;
import teaselib.userinterface.MediaRenderer;
import teaselib.userinterface.MediaRendererThread;
import teaselib.util.RenderDesktopItem;

public class RenderMessage extends MediaRendererThread {

    private final Message message;
    private final TextToSpeechPlayer speechSynthesizer;
    private String displayImage;
    private final Set<String> hints = new HashSet<String>();

    private final static long DELAYBETWEENPARAGRAPHS = 500;
    private final static long DELAYATENDOFTEXT = 2000;

    public RenderMessage(Message message, TextToSpeechPlayer speechSynthesizer,
            String displayImage, Collection<String> hints) {
        if (message == null) {
            throw new NullPointerException();
        }
        this.message = message;
        this.speechSynthesizer = speechSynthesizer;
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
                // // Set image and no text
                // if (displayImage == TeaseScript.NoImage) {
                // teaseLib.host.show(null, null);
                // } else if (displayImage != TeaseScript.DominantImage) {
                // teaseLib.host.show(imageIterator.next(), null);
                // } else {
                // String[] hintArray = new String[hints.size()];
                // hintArray = hints.toArray(hintArray);
                // imageIterator.hint(hintArray);
                // teaseLib.host.show(imageIterator.next(), null);
                // }
                showImageAndText(null, hints);
            } else {
                StringBuilder accumulatedText = null;
                boolean append = false;
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
                RenderSound soundRenderer = null;
                String mood = Mood.Neutral;
                boolean appendToItem = false;
                for (Iterator<Part> it = message.iterator(); it.hasNext();) {
                    Set<String> additionalHints = new HashSet<String>();
                    additionalHints.addAll(hints);
                    // Handle message commands
                    Part part;
                    part = it.next();
                    TeaseLib.log(part.type.toString() + ": " + part.value);
                    if (part.type == Message.Type.Image) {
                        displayImage = part.value;
                    } else if (part.type == Message.Type.Sound) {
                        // Play sound, continue message execution
                        soundRenderer = new RenderSound(part.value);
                        soundRenderer.render(teaseLib);
                        // use AwaitSoundCompletion to wait for sound completion
                    } else if (part.type == Message.Type.DesktopItem) {
                        new RenderDesktopItem(part.value).render(teaseLib);
                    } else if (part.type == Message.Type.Mood) {
                        // Mood
                        mood = part.value;
                    } else if (part.type == Message.Type.Keyword) {
                        doKeyword(soundRenderer, part);
                    } else if (part.type == Message.Type.Delay) {
                        // Pause
                        doPause(part);
                    } else if (part.type == Message.Type.Item) {
                        accumulatedText = accumulateText(accumulatedText, "°",
                                false);
                        appendToItem = true;
                    } else if (part.type == Message.Type.Exec) {
                        // Exec
                        doExec(part);
                    } else {
                        // Text detected
                        String text = part.value;
                        accumulatedText = accumulateText(accumulatedText, text,
                                append);
                        // Update text
                        additionalHints.add(mood);
                        showImageAndText(accumulatedText.toString(),
                                additionalHints);
                        // First message shown - start part completed
                        startCompleted();
                        if (speechSynthesizer != null) {
                            speechSynthesizer.setMood(mood,
                                    prerenderedSpeechItems);
                            speechSynthesizer.speak(text,
                                    prerenderedSpeechItems);
                        }
                        pauseAfterParagraph(it);
                    }
                    if (endThread) {
                        break;
                    }
                    // Find out whether to append the next sentence to the same
                    // or a new line
                    if (appendToItem) {
                        append = true;
                        appendToItem = false;
                    } else {
                        append = canAppend(part.value);
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

    private void doKeyword(RenderSound soundRenderer, Part part) {
        String keyword = part.value;
        if (keyword == TeaseScript.DominantImage) {
            // Mistress image
            displayImage = TeaseScript.DominantImage;
        } else if (keyword == TeaseScript.NoImage) {
            // No image
            displayImage = TeaseScript.NoImage;
        } else if (keyword == Message.ShowChoices) {
            // Complete the mandatory part of the message
            mandatoryCompleted();
        } else if (keyword == Message.AwaitSoundCompletion) {
            // Complete the mandatory part of the message
            soundRenderer.completeMandatory();
        } else {
            // Unimplemented keyword
            throw new IllegalArgumentException("Unimplemented keyword: "
                    + keyword);
        }
    }

    private void doPause(Part part) {
        String args = removeCommandNameFromValue(part);
        if (args.isEmpty()) {
            // Fixed pause
            teaseLib.sleep(DELAYATENDOFTEXT, TimeUnit.MILLISECONDS);
        } else {
            try {
                String[] argv = args.split(" ");
                if (argv.length == 1) {
                    double delay = Double.parseDouble(args) * 1000;
                    teaseLib.sleep((int) delay, TimeUnit.MILLISECONDS);
                } else {
                    double delayFrom = Double.parseDouble(argv[0]) * 1000;
                    double delayTo = Double.parseDouble(argv[1]) * 1000;
                    teaseLib.sleep(
                            teaseLib.random((int) delayFrom, (int) delayTo),
                            TimeUnit.MILLISECONDS);
                }
            } catch (NumberFormatException ignore) {
                // Fixed pause
                teaseLib.sleep(DELAYATENDOFTEXT, TimeUnit.MILLISECONDS);
            }
        }
    }

    private void doExec(Part part) {
        String path = removeCommandNameFromValue(part);
        MediaRenderer desktopItem = new RenderDesktopItem(path);
        desktopItem.render(teaseLib);
    }

    private static String removeCommandNameFromValue(Part part) {
        String value = part.value;
        if (value.equalsIgnoreCase(part.type.toString())) {
            return "";
        } else {
            return part.value.substring(part.type.toString().length() + 1);
        }
    }

    private static StringBuilder accumulateText(StringBuilder accumulatedText,
            String text, boolean concatenate) {
        if (accumulatedText == null) {
            accumulatedText = new StringBuilder(text);
        } else if (concatenate) {
            accumulatedText.append(" ");
            accumulatedText.append(text);
        } else {
            accumulatedText.append("\n\n");
            accumulatedText.append(text);
        }
        return accumulatedText;
    }

    private void pauseAfterParagraph(Iterator<Message.Part> it) {
        final boolean lastParagraph = !it.hasNext();
        if (lastParagraph) {
            // Interaction should start before the final delay
            mandatoryCompleted();
            teaseLib.sleep(DELAYATENDOFTEXT, TimeUnit.MILLISECONDS);
            allCompleted();
            endThread = true;
        } else {
            teaseLib.sleep(DELAYBETWEENPARAGRAPHS, TimeUnit.MILLISECONDS);
        }
    }

    private static boolean canAppend(String s) {
        char ending = s.isEmpty() ? ' ' : s.charAt(s.length() - 1);
        return ending == ',' || ending == ';';
    }

    private void showImageAndText(String text, Set<String> additionalHints) {
        // Apply image and text
        Image image;
        try {
            if (displayImage == TeaseScript.DominantImage) {
                Images images = message.actor.images;
                if (images != null) {
                    String[] hintArray = new String[additionalHints.size()];
                    hintArray = additionalHints.toArray(hintArray);
                    images.hint(hintArray);
                    image = teaseLib.resources.image(images.next());
                } else {
                    image = null;
                    TeaseLib.log("Dominant images missing - please initialize");
                }
            } else if (displayImage == TeaseScript.NoImage) {
                image = null;
            } else {
                // TODO Cache image or detect reusage, since
                // currently the same image is reloaded for
                // each
                // text part (usually when setting the image
                // outside
                // the message)
                image = teaseLib.resources.image(displayImage);
            }
        } catch (Exception e) {
            text = text + "\n\n" + e.getClass() + ": " + e.getMessage() + "\n";
            image = null;
            TeaseLib.log(this, e);
        }
        teaseLib.host.show(image, text);
    }

    @Override
    public String toString() {
        long delay = 0;
        Collection<Part> paragraphs = message.getParts();
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
