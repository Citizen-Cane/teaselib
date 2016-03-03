package teaselib.core;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import teaselib.Config;
import teaselib.Message;
import teaselib.Message.Part;
import teaselib.Mood;
import teaselib.TeaseLib;
import teaselib.core.texttospeech.TextToSpeech;
import teaselib.core.texttospeech.TextToSpeechPlayer;

public class RenderMessage extends MediaRendererThread {
    private final static long DELAYBETWEENPARAGRAPHS = 500;
    private final static long DELAYATENDOFTEXT = 2000;

    static final Set<Message.Type> logSpecialMessageTypes = new HashSet<Message.Type>(
            Arrays.asList(Message.Type.Text, Message.Type.Image,
                    Message.Type.Mood));

    private final ResourceLoader resources;
    private final Message message;
    private final TextToSpeechPlayer ttsPlayer;
    private final boolean speak;
    MediaRendererThread speechRenderer = null;
    MediaRendererThread speechRendererInProgress = null;
    RenderSound soundRenderer = null;

    private String displayImage = null;

    private final Set<MediaRendererThread> interuptableAudio = new HashSet<MediaRendererThread>();

    public RenderMessage(ResourceLoader resources, Message message,
            TextToSpeechPlayer ttsPlayer, TeaseLib teaseLib) {
        super(teaseLib);
        if (message == null) {
            throw new NullPointerException();
        }
        this.resources = resources;
        // replay pre-recorded speech or use TTS
        if (ttsPlayer != null) {
            if (ttsPlayer.prerenderedSpeechAvailable(message.actor)) {
                // Don't use TTS, even if pre-recorded speech is missing
                message = ttsPlayer.getPrerenderedMessage(message, resources);
            }
        }
        this.message = message;
        this.ttsPlayer = ttsPlayer;
        this.speak = ttsPlayer != null;
    }

    public Message getMessage() {
        return message;
    }

    @Override
    public void renderMedia() throws InterruptedException {
        try {
            if (replayPosition == Position.FromStart) {
                renderMessage(message, true);
            } else {
                Message lastSection = getLastSection(message);
                if (replayPosition == Position.FromMandatory) {
                    renderMessage(lastSection, true);
                } else {
                    renderMessage(lastSection, false);
                }
            }
        } catch (ScriptInterruptedException e) {
            // Expected
        } catch (Throwable t) {
            teaseLib.log.error(this, t);
        }
    }

    private static Message getLastSection(Message message) {
        Message lastSection = new Message(message.actor);
        Message.Parts parts = message.getParts();
        int index = parts.size();
        while (index-- > 0) {
            if (parts.get(index).type == Message.Type.Text) {
                break;
            }
        }
        // No text
        if (index < 0) {
            return message;
        }
        // Get the modifiers for this text part (image, sound, mood, ...)
        while (index-- > 0) {
            if (parts.get(index).type == Message.Type.Text) {
                // Start the last section after the second last text part
                index++;
                break;
            }
        }
        // One text element -> whole message
        if (index < 0) {
            index = 0;
        }
        // Copy message header (all but skip desktop items and delay before the
        // text)
        boolean afterText = false;
        for (int i = index; i < parts.size(); i++) {
            Message.Part part = parts.get(i);
            if (part.type == Message.Type.DesktopItem) {
                // skip
            } else if (part.type == Message.Type.Delay && !afterText) {
                // skip
            } else {
                lastSection.add(message.new Part(part.type, part.value));
            }
            if (part.type == Message.Type.Text) {
                afterText = true;
            }
        }
        return lastSection;
    }

    private void renderMessage(Message message, boolean speakText)
            throws IOException {
        if (message.isEmpty()) {
            // // Show image but no text
            doText(null, false, null, Mood.Neutral);
        } else {
            StringBuilder accumulatedText = new StringBuilder();
            boolean append = false;
            String mood = Mood.Neutral;
            boolean appendToItem = false;
            // Process message parts
            for (Iterator<Part> it = message.iterator(); it.hasNext();) {
                Part part = it.next();
                if (!logSpecialMessageTypes.contains(part.type)) {
                    teaseLib.transcript
                            .info("" + part.type.name() + " = " + part.value);
                }
                teaseLib.log.info(part.type.toString() + ": " + part.value);
                // TODO Works only if the last part is a text part
                final boolean lastParagraph = !it.hasNext();
                // Handle message commands
                if (part.type == Message.Type.Image) {
                    displayImage = part.value;
                } else if (part.type == Message.Type.BackgroundSound) {
                    // Play sound, continue message execution
                    synchronized (interuptableAudio) {
                        soundRenderer = new RenderSound(resources, part.value,
                                teaseLib);
                        soundRenderer.render();
                        interuptableAudio.add(soundRenderer);
                    }
                    // use awaitSoundCompletion keyword to wait for sound
                    // completion
                } else if (part.type == Message.Type.Sound) {
                    // Play sound, wait until finished
                    RenderSound sound = new RenderSound(resources, part.value,
                            teaseLib);
                    synchronized (interuptableAudio) {
                        sound.render();
                        interuptableAudio.add(sound);
                    }
                    sound.completeAll();
                    synchronized (interuptableAudio) {
                        interuptableAudio.remove(sound);
                    }
                } else if (part.type == Message.Type.Speech) {
                    if (speakText) {
                        speechRenderer = new RenderPrerecordedSpeech(part.value,
                                getParagraphPause(lastParagraph), resources,
                                teaseLib);
                    }
                } else if (part.type == Message.Type.DesktopItem) {
                    // Remove optional keyword
                    String path = part.value.toLowerCase()
                            .startsWith(Message.ShowOnDesktop) ? part.value
                                    .substring(Message.ShowOnDesktop.length())
                                    .trim() : part.value;
                    URI uri = resources.uri(path);
                    if (uri != null) {
                        new RenderDesktopItem(uri, teaseLib).render();
                    } else {
                        // Text might be treated as a desktop item,
                        // because our file detection code is too lax
                        // (should check whether a file with that name
                        // exists, but that might be too strict)
                        // -> if the url to the desktop item cannot be
                        // retrieved (the desktop item doesn't exist),
                        // we'll just display the part as text,
                        // which is the most right thing in this case

                        throw new IllegalStateException();
                        // String prompt = part.value;
                        // doText(accumulatedText, append, mood, prompt);
                        // if (ttsPlayer != null && !prerenderedSpeechAvailable)
                        // {
                        // ttsPlayer.speak(message.actor, prompt, mood);
                        // }
                        // pauseAfterParagraph(lastParagraph, ttsPlayer);
                    }
                } else if (part.type == Message.Type.Mood) {
                    // Mood
                    mood = part.value;
                } else if (part.type == Message.Type.Keyword) {
                    doKeyword(part);
                } else if (part.type == Message.Type.Delay) {
                    // Pause
                    doDelay(part);
                } else if (part.type == Message.Type.Item) {
                    accumulateText(accumulatedText, "°", false);
                    appendToItem = true;
                } else { // (part.type == Message.Type.Text)
                    String prompt = part.value;
                    doTextAndSpeech(accumulatedText, prompt, append, mood,
                            lastParagraph, speakText);
                }
                if (task.isCancelled()) {
                    break;
                }
                if (lastParagraph) {
                    break;
                }
                // Find out whether to append the next sentence to the same
                // or a new line
                if (appendToItem) {
                    append = true;
                    appendToItem = false;
                } else if (part.type == Message.Type.Text) {
                    append = canAppend(part.value);
                }
            }
            // Finished all parts
            completeSpeech(true);
            allCompleted();

        }
    }

    private void doTextAndSpeech(StringBuilder accumulatedText, String prompt,
            boolean append, String mood, final boolean lastParagraph,
            boolean speakText) {
        doText(accumulatedText, append, prompt, mood);
        if (ttsPlayer != null && speechRenderer == null) {
            if (speakText) {
                speechRenderer = new RenderTTSSpeech(ttsPlayer, message.actor,
                        prompt, mood, getParagraphPause(lastParagraph),
                        teaseLib);
            }
        }
        // Start next
        if (speechRenderer != null) {
            synchronized (interuptableAudio) {
                // Play sound, wait until finished
                speechRenderer.render();
                // Automatically interrupt
                interuptableAudio.add(speechRenderer);
            }
            speechRendererInProgress = speechRenderer;
            speechRenderer = null;
        }

    }

    private void completeSpeech(final boolean lastParagraph) {
        if (speechRendererInProgress != null) {
            speechRendererInProgress.completeMandatory();
        }
        if (lastParagraph) {
            mandatoryCompleted();
        }
        if (speechRendererInProgress != null) {
            speechRendererInProgress.completeAll();
            synchronized (interuptableAudio) {
                interuptableAudio.remove(speechRendererInProgress);
            }
            speechRendererInProgress = null;
        }
    }

    private void doText(StringBuilder accumulatedText, boolean append,
            String prompt, String mood) {
        accumulateText(accumulatedText, prompt, append);
        if (message.actor.images.contains(displayImage)
                && mood != Mood.Neutral) {
            teaseLib.transcript.info("mood = " + mood);
        }

        String path = displayImage == Message.NoImage ? null : displayImage;
        // log image to transcript if not an actor's image
        if (path == null) {
            teaseLib.transcript.info(Message.NoImage);
        } else {
            if (message.actor.images.contains(displayImage)) {
                teaseLib.transcript.debug("image = '" + path + "'");
            } else {
                teaseLib.transcript.info("image = '" + path + "'");
            }
        }
        // Fetch the image bytes
        byte[] imageBytes = null;
        String text = accumulatedText.toString();
        if (path != null) {
            try {
                imageBytes = getImageBytes(path);
            } catch (Exception e) {
                text = text + "\n\n" + e.getClass() + ": " + e.getMessage()
                        + "\n";
                teaseLib.log.error(this, e);
            }
        }
        // speech in progress never refers to the last paragraph
        completeSpeech(false);
        teaseLib.host.show(imageBytes, text);
        teaseLib.transcript.info(">> " + prompt);
        // First message shown - start part completed
        startCompleted();
    }

    private static long getParagraphPause(boolean lastParagraph) {
        return lastParagraph ? DELAYATENDOFTEXT : DELAYBETWEENPARAGRAPHS;
    }

    private void doKeyword(Part part) {
        String keyword = part.value;
        if (keyword == Message.DominantImage) {
            // Image has to be resolved in preprocessMessage
            throw new IllegalStateException(keyword);
        } else if (keyword == Message.NoImage) {
            // No image
            displayImage = Message.NoImage;
        } else if (keyword == Message.ShowChoices) {
            // Complete the mandatory part of the message
            mandatoryCompleted();
        } else if (keyword == Message.AwaitSoundCompletion) {
            // Complete the mandatory part of the message
            // TODO Complete all audio but speech
            soundRenderer.completeMandatory();
            synchronized (interuptableAudio) {
                interuptableAudio.remove(soundRenderer);
            }
        } else {
            // Unimplemented keyword
            throw new IllegalArgumentException(
                    "Unimplemented keyword: " + keyword);
        }
    }

    private void doDelay(Part part) {
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

    private static String removeCommandNameFromValue(Part part) {
        String value = part.value;
        if (value.equalsIgnoreCase(part.type.toString())) {
            return "";
        } else {
            return part.value.substring(part.type.toString().length() + 1);
        }
    }

    private static void accumulateText(StringBuilder accumulatedText,
            String text, boolean concatenate) {
        if (accumulatedText.length() == 0) {
            accumulatedText.append(text);
        } else if (concatenate) {
            accumulatedText.append(" ");
            accumulatedText.append(text);
        } else {
            accumulatedText.append("\n\n");
            accumulatedText.append(text);
        }
    }

    private static boolean canAppend(String s) {
        String ending = s.isEmpty() ? " "
                : s.substring(s.length() - 1, s.length());
        return Message.MainClauseAppendableCharacters.contains(ending);
    }

    private byte[] getImageBytes(String path) throws IOException {
        InputStream resource = null;
        byte[] imageBytes = null;
        try {
            resource = resources.getResource(path);
            imageBytes = convertInputStreamToByte(resource);
        } catch (IOException e) {
            if (!teaseLib.getBoolean(Config.Namespace,
                    Config.Debug.IgnoreMissingResources)) {
                throw e;
            }
        } finally {
            if (resource != null) {
                resource.close();
            }
        }
        return imageBytes;
    }

    private static byte[] convertInputStreamToByte(InputStream is)
            throws IOException {
        byte[] buffer = new byte[8192];
        int bytesRead;
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        while ((bytesRead = is.read(buffer)) != -1) {
            output.write(buffer, 0, bytesRead);
        }
        return output.toByteArray();
    }

    @Override
    public String toString() {
        long delay = 0;
        Message.Parts paragraphs = message.getParts();
        for (Iterator<Part> it = paragraphs.iterator(); it.hasNext();) {
            Part paragraph = it.next();
            delay += TextToSpeech.getEstimatedSpeechDuration(paragraph.value);
            if (it.hasNext()) {
                delay += DELAYBETWEENPARAGRAPHS;
            } else {
                delay += DELAYATENDOFTEXT;
            }
        }
        String messageText = message.toHashString().replace("\n", " ");
        int length = 40;
        return "Estimated delay = "
                + String.format("%.2f", (double) delay / 1000) + " Message = "
                + (messageText.length() > length
                        ? messageText.substring(0, length) + "..."
                        : messageText);
    }

    @Override
    public void interrupt() {
        // Cancel TTS speech
        if (!hasCompletedMandatory()) {
            if (ttsPlayer != null) {
                ttsPlayer.stop();
            }
        }
        synchronized (interuptableAudio) {
            for (MediaRendererThread sound : interuptableAudio) {
                sound.interrupt();
            }
        }
        super.interrupt();
    }
}
