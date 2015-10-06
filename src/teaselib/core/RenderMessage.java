package teaselib.core;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import teaselib.Config;
import teaselib.Message;
import teaselib.Message.Part;
import teaselib.Mood;
import teaselib.TeaseLib;
import teaselib.core.MediaRenderer.Replay;
import teaselib.core.texttospeech.TextToSpeech;
import teaselib.core.texttospeech.TextToSpeechPlayer;

public class RenderMessage extends MediaRendererThread implements Replay {
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
            TeaseLib.log(this, t);
            teaseLib.host.show(null, t.getMessage());
        }
    }

    private static Message getLastSection(Message message) {
        Message lastSection = new Message(message.actor);
        final List<Part> parts = message.getParts();
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
            TextToSpeechPlayer speechSynthesizer) throws IOException {
        if (message.isEmpty()) {
            // // Show image but no text
            showImageAndText(null, hints);
        } else {
            StringBuilder accumulatedText = new StringBuilder();
            boolean append = false;
            // Start speaking a message, replay prerecorded items or speak
            // with TTS later
            final Iterator<String> prerenderedSpeechItems;
            if (speechSynthesizer != null) {
                prerenderedSpeechItems = speechSynthesizer.selectVoice(
                        resources, message);
            } else {
                prerenderedSpeechItems = null;
            }
            // Process message paragraphs
            RenderSound soundRenderer = null;
            String mood = defaultMood;
            boolean appendToItem = false;
            for (Iterator<Part> it = message.iterator(); it.hasNext();) {
                Set<String> additionalHints = new HashSet<String>();
                additionalHints.addAll(hints);
                // Handle message commands
                Part part;
                part = it.next();
                final boolean lastParagraph = !it.hasNext();
                TeaseLib.log(part.type.toString() + ": " + part.value);
                if (part.type == Message.Type.Image) {
                    displayImage = part.value;
                } else if (part.type == Message.Type.Sound) {
                    // Play sound, continue message execution
                    soundRenderer = new RenderSound(resources, part.value);
                    soundRenderer.render(teaseLib);
                    // use AwaitSoundCompletion to wait for sound completion
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
                        mood = doTextAndPause(accumulatedText, append,
                                speechSynthesizer, prerenderedSpeechItems,
                                mood, lastParagraph, additionalHints, part);
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
                    mood = doTextAndPause(accumulatedText, append,
                            speechSynthesizer, prerenderedSpeechItems, mood,
                            lastParagraph, additionalHints, part);
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

    private String doTextAndPause(StringBuilder accumulatedText,
            boolean append, TextToSpeechPlayer speechSynthesizer,
            final Iterator<String> prerenderedSpeechItems, String mood,
            boolean lastParagraph, Set<String> additionalHints, Part part)
            throws IOException {
        doText(accumulatedText, part, append, speechSynthesizer,
                prerenderedSpeechItems, mood, additionalHints);
        pauseAfterParagraph(lastParagraph, speechSynthesizer);
        mood = resetMood(mood);
        return mood;
    }

    private void doText(StringBuilder accumulatedText, Part part,
            boolean append, TextToSpeechPlayer speechSynthesizer,
            final Iterator<String> prerenderedSpeechItems, String mood,
            Set<String> additionalHints) throws IOException {
        String prompt = part.value;
        accumulateText(accumulatedText, prompt, append);
        // Update text
        additionalHints.add(mood);
        showImageAndText(accumulatedText.toString(), additionalHints);
        // First message shown - start part completed
        startCompleted();
        if (speechSynthesizer != null) {
            if (prerenderedSpeechItems != null) {
                speechSynthesizer.play(resources, message.actor, prompt,
                        prerenderedSpeechItems);
            } else {
                speechSynthesizer.speak(message.actor, prompt, mood);
            }
        }
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
            throw new IllegalArgumentException("Unimplemented keyword: "
                    + keyword);
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
            desktopItem.render(teaseLib);
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
        byte[] imageBytes = null;
        try {
            try {
                if (displayImage == Message.DominantImage) {
                    Images images = message.actor.images;
                    if (images != null) {
                        String[] hintArray = new String[additionalHints.size()];
                        hintArray = additionalHints.toArray(hintArray);
                        images.hint(hintArray);
                        imageBytes = convertInputStreamToByte(resources
                                .getResource(images.next()));
                    } else {
                        imageBytes = null;
                        TeaseLib.log("Actor '" + message.actor.name
                                + "': images missing - please initialize");
                    }
                } else if (displayImage == Message.NoImage) {
                    imageBytes = null;
                } else {
                    // TODO Cache image or detect reusage, since
                    // currently the same image is reloaded for
                    // each
                    // text part (usually when setting the image
                    // outside
                    // the message)
                    final InputStream resource = resources
                            .getResource(displayImage);
                    try {
                        imageBytes = convertInputStreamToByte(resource);
                    } finally {
                        resource.close();
                    }
                }
            } catch (IOException e) {
                if (!teaseLib.getBoolean(Config.Namespace,
                        Config.Debug.IgnoreMissingResources)) {
                    throw e;
                }
            }
        } catch (Exception e) {
            text = text + "\n\n" + e.getClass() + ": " + e.getMessage() + "\n";
            imageBytes = null;
            TeaseLib.log(this, e);
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
    public void join() {
        // Cancel ongoing TTS speech
        if (ttsPlayer != null) {
            ttsPlayer.stop();
        }
        // Cancel prerecorded TTS speech
        // as well as any other sounds
        teaseLib.host.stopSounds();
        // TODO Only stop speech and sounds that
        // have been started by this message renderer
        super.join();
    }
}
