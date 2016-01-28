package teaselib.core;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import teaselib.Config;
import teaselib.Message;
import teaselib.Message.Part;
import teaselib.Message.Type;
import teaselib.Mood;
import teaselib.core.speechrecognition.SpeechRecognition;
import teaselib.core.speechrecognition.SpeechRecognizer;
import teaselib.core.texttospeech.TextToSpeech;
import teaselib.core.texttospeech.TextToSpeechPlayer;

public class RenderMessage extends MediaRendererThread {
    private final static long DELAYBETWEENPARAGRAPHS = 500;
    private final static long DELAYATENDOFTEXT = 2000;

    private final ResourceLoader resources;
    private final Message message;
    private final TextToSpeechPlayer ttsPlayer;
    private final Set<String> hints = new HashSet<String>();

    private String displayImage;
    private String defaultMood = Mood.Neutral;

    public RenderMessage(ResourceLoader resources, Message message,
            TextToSpeechPlayer ttsPlayer, String displayImage,
            Collection<String> hints) {
        if (message == null) {
            throw new NullPointerException();
        }
        this.resources = resources;
        this.message = message;
        this.ttsPlayer = ttsPlayer;
        this.displayImage = displayImage;
        hints.addAll(hints);
        // Default mood?
        for (String hint : hints) {
            if (Mood.isMood(hint)) {
                defaultMood = hint;
                break;
            }
        }
    }

    public Message getMessage() {
        return message;
    }

    @Override
    public void render() throws InterruptedException {
        try {
            if (replayPosition == Position.FromStart) {
                renderMessage(message, ttsPlayer);
            } else {
                Message lastSection = getLastSection(message);
                if (replayPosition == Position.FromMandatory) {
                    renderMessage(lastSection, ttsPlayer);
                } else {
                    renderMessage(lastSection, null);
                }
            }
        } catch (ScriptInterruptedException e) {
            // Expected
        } catch (Throwable t) {
            teaseLib.log.error(this, t);
            teaseLib.host.show(null, t.getMessage());
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

    private void renderMessage(Message message,
            TextToSpeechPlayer ttsPlayer) throws IOException {
        if (message.isEmpty()) {
            // // Show image but no text
            showImageAndText(null, hints);
        } else {
            StringBuilder accumulatedText = new StringBuilder();
            boolean append = false;
            // Start speaking a message, replay prerecorded items or speak
            // with TTS later
            final boolean prerenderedSpeechAvailable;
            if (ttsPlayer != null) {
                if (ttsPlayer
                        .prerenderedSpeechAvailable(message.actor)) {
                    message = ttsPlayer.getPrerenderedMessage(message,
                            resources);
                    prerenderedSpeechAvailable = true;
                } else {
                    prerenderedSpeechAvailable = false;
                }
            } else {
                prerenderedSpeechAvailable = false;
            }
            // Process message paragraphs
            RenderSound soundRenderer = null;
            String mood = defaultMood;
            boolean appendToItem = false;
            for (Iterator<Part> it = message.iterator(); it.hasNext();) {
                Part part = it.next();
                if (part.type != Type.Text && part.type != Type.Image) {
                    teaseLib.transcript
                            .info("" + part.type.name() + " = " + part.value);
                }
                teaseLib.log.info(part.type.toString() + ": " + part.value);
                // Handle message commands
                Set<String> additionalHints = new HashSet<String>();
                additionalHints.addAll(hints);
                final boolean lastParagraph = !it.hasNext();
                if (part.type == Message.Type.Image) {
                    displayImage = part.value;
                } else if (part.type == Message.Type.BackgroundSound) {
                    // Play sound, continue message execution
                    soundRenderer = new RenderSound(resources, part.value);
                    soundRenderer.render(teaseLib);
                    // use awaitSoundCompletion to wait for sound completion
                } else if (part.type == Message.Type.Sound) {
                    // Play sound, wait until finished
                    RenderSound audio = new RenderSound(resources, part.value);
                    audio.render(teaseLib);
                    audio.completeAll();
                } else if (part.type == Message.Type.Speech) {
                    // Disable speech recognition
                    final SpeechRecognition speechRecognizer = SpeechRecognizer.instance
                            .get(message.actor.locale);
                    // Suspend speech recognition while speaking,
                    // to avoid wrong recognitions
                    // - and the mistress speech isn't to be interrupted anyway
                    SpeechRecognition.completeSpeechRecognitionInProgress();
                    boolean reactivateSpeechRecognition = speechRecognizer != null
                            && speechRecognizer.isActive();
                    try {
                        if (reactivateSpeechRecognition) {
                            if (speechRecognizer != null) {
                                speechRecognizer.stopRecognition();
                            }
                        }
                        // Play sound, wait until finished
                        RenderSound audio = new RenderSound(resources,
                                part.value);
                        audio.render(teaseLib);
                        audio.completeAll();
                    } finally {
                        // resume SR if necessary
                        if (reactivateSpeechRecognition) {
                            if (speechRecognizer != null) {
                                speechRecognizer.resumeRecognition();
                            }
                        }
                    }
                } else if (part.type == Message.Type.DesktopItem) {
                    final URI uri = resources.uri(part.value);
                    if (uri != null) {
                        new RenderDesktopItem(uri).render(teaseLib);
                    } else {
                        // text might be treated as a desktop item,
                        // because our file detection code is too lax
                        // (should check whether a file with that name
                        // exists, but that might be too strict)
                        // -> if the url to the desktop item cannot be
                        // retrieved (the desktop item doesn't exist),
                        // we'll just display the part as text,
                        // which is the most right thing in this case
                        String prompt = part.value;
                        doTextAndPause(accumulatedText, append, mood,
                                additionalHints, prompt);
                        if (ttsPlayer != null
                                && !prerenderedSpeechAvailable) {
                            ttsPlayer.speak(message.actor, prompt,
                                    mood);
                        }
                        mood = pauseAfterText(mood, lastParagraph,
                                ttsPlayer);
                    }
                } else if (part.type == Message.Type.Mood) {
                    // Mood
                    mood = part.value;
                } else if (part.type == Message.Type.Keyword) {
                    doKeyword(soundRenderer, part);
                } else if (part.type == Message.Type.Delay) {
                    // Pause
                    doDelay(part);
                } else if (part.type == Message.Type.Item) {
                    accumulateText(accumulatedText, "°", false);
                    appendToItem = true;
                    mood = resetMood(mood);
                } else if (part.type == Message.Type.Exec) {
                    // Exec
                    doExec(part);
                } else {
                    String prompt = part.value;
                    doTextAndPause(accumulatedText, append, mood,
                            additionalHints, prompt);
                    if (ttsPlayer != null
                            && !prerenderedSpeechAvailable) {
                        ttsPlayer.speak(message.actor, prompt, mood);
                    }
                    mood = pauseAfterText(mood, lastParagraph,
                            ttsPlayer);
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
    }

    private void doTextAndPause(StringBuilder accumulatedText, boolean append,
            String mood, Set<String> additionalHints, String prompt) {
        accumulateText(accumulatedText, prompt, append);
        // Update text
        additionalHints.add(mood);
        if ((displayImage == Message.DominantImage && mood != defaultMood)) {
            teaseLib.transcript.info("mood = " + mood);
        }
        showImageAndText(accumulatedText.toString(), additionalHints);
        teaseLib.transcript.info(">> " + prompt);
        // First message shown - start part completed
        startCompleted();
    }

    private String pauseAfterText(String mood, boolean lastParagraph,
            TextToSpeechPlayer speechSynthesizer) {
        pauseAfterParagraph(lastParagraph, speechSynthesizer);
        mood = resetMood(mood);
        return mood;
    }

    private void pauseAfterParagraph(boolean lastParagraph,
            TextToSpeechPlayer speechSynthesizer) {
        final boolean spokenMessage = speechSynthesizer != null;
        if (lastParagraph) {
            if (spokenMessage) {
                // Interaction should start before the final delay
                mandatoryCompleted();
                teaseLib.sleep(DELAYATENDOFTEXT, TimeUnit.MILLISECONDS);
            }
            allCompleted();
            endThread = true;
        } else {
            if (spokenMessage) {
                teaseLib.sleep(DELAYBETWEENPARAGRAPHS, TimeUnit.MILLISECONDS);
            }
        }
    }

    private String resetMood(String mood) {
        // Reset mood after applying it once
        if (mood != Mood.Reading) {
            mood = defaultMood;
        }
        return mood;
    }

    private void doKeyword(RenderSound soundRenderer, Part part) {
        String keyword = part.value;
        if (keyword == Message.DominantImage) {
            // Mistress image
            displayImage = Message.DominantImage;
        } else if (keyword == Message.NoImage) {
            // No image
            displayImage = Message.NoImage;
        } else if (keyword == Message.ShowChoices) {
            // Complete the mandatory part of the message
            mandatoryCompleted();
        } else if (keyword == Message.AwaitSoundCompletion) {
            // Complete the mandatory part of the message
            soundRenderer.completeMandatory();
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

    private void doExec(Part part) {
        String path = removeCommandNameFromValue(part);
        URI uri = resources.uri(path);
        if (uri != null) {
            MediaRenderer desktopItem = new RenderDesktopItem(uri);
            try {
                desktopItem.render(teaseLib);
            } catch (IOException e) {
                teaseLib.log.error(this, e);
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
        char ending = s.isEmpty() ? ' ' : s.charAt(s.length() - 1);
        return ending == ',' || ending == ';';
    }

    private void showImageAndText(String text, Set<String> additionalHints) {
        // Apply image and text
        final String path;
        if (displayImage == Message.DominantImage) {
            Images images = message.actor.images;
            if (images != null) {
                String[] hintArray = new String[additionalHints.size()];
                hintArray = additionalHints.toArray(hintArray);
                images.hint(hintArray);
                path = images.next();
                if (path == null && !teaseLib.getBoolean(Config.Namespace,
                        Config.Debug.IgnoreMissingResources)) {
                    teaseLib.log.info("Actor '" + message.actor.name
                            + "': images missing - please initialize");
                }
            } else if (!teaseLib.getBoolean(Config.Namespace,
                    Config.Debug.IgnoreMissingResources)) {
                teaseLib.log.info("Actor '" + message.actor.name
                        + "': images missing - please initialize");
                path = null;
            } else {
                path = null;
            }
        } else if (displayImage == Message.NoImage) {
            path = null;
        } else {
            // TODO Cache image or detect reusage, since
            // currently the same image is reloaded again for
            // each text part
            // (usually when setting the image outside the message)
            path = displayImage;
        }
        if (path == null) {
            teaseLib.transcript.info(Message.NoImage);
        } else {
            if (displayImage == Message.DominantImage) {
                teaseLib.transcript.debug("image = '" + path + "'");
            } else {
                teaseLib.transcript.info("image = '" + path + "'");
            }

        }
        byte[] imageBytes = null;
        if (path != null) {
            try {
                InputStream resource = null;
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
            } catch (Exception e) {
                text = text + "\n\n" + e.getClass() + ": " + e.getMessage()
                        + "\n";
                teaseLib.log.error(this, e);
            }
        }
        teaseLib.host.show(imageBytes, text);
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
        return "Estimated delay = "
                + String.format("%.2f", (double) delay / 1000);
    }

    @Override
    public void join() {
        // Cancel TTS speech
        if (!hasCompletedMandatory()) {
            if (ttsPlayer != null) {
                ttsPlayer.stop();
            }
        }
        // TODO Sounds should be cancelled by each sound renderer
        // -> Need to track sounds renderers and interrupt them here
        // Interrupt sound renderes by overwriting interrupt()
        teaseLib.host.stopSounds();
        super.join();
    }
}
