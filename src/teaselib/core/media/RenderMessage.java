package teaselib.core.media;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import teaselib.Config;
import teaselib.Message;
import teaselib.Message.Part;
import teaselib.Mood;
import teaselib.core.Prefetcher;
import teaselib.core.ResourceLoader;
import teaselib.core.TeaseLib;
import teaselib.core.texttospeech.TextToSpeech;
import teaselib.core.texttospeech.TextToSpeechPlayer;

public class RenderMessage extends MediaRendererThread {
    private static final Logger logger = LoggerFactory
            .getLogger(RenderMessage.class);

    private final static long DELAY_BETWEEN_PARAGRAPHS = 500;
    private final static long DELAY_AT_END_OF_MESSAGE = 2000;

    static final Set<Message.Type> logSpecialMessageTypes = new HashSet<Message.Type>(
            Arrays.asList(Message.Type.Text, Message.Type.Image,
                    Message.Type.Mood));

    private final ResourceLoader resources;
    private final Message message;
    private final TextToSpeechPlayer ttsPlayer;
    MediaRendererThread speechRenderer = null;
    MediaRendererThread speechRendererInProgress = null;
    RenderSound soundRenderer = null;

    private String displayImage = null;
    private final Prefetcher<byte[]> imageFetcher = new Prefetcher<byte[]>();

    private final Set<MediaRendererThread> interuptableAudio = new HashSet<MediaRendererThread>();

    public RenderMessage(ResourceLoader resources, Message message,
            TextToSpeechPlayer ttsPlayer, TeaseLib teaseLib) {
        super(teaseLib);
        if (message == null) {
            throw new NullPointerException();
        }
        this.resources = resources;
        this.message = message;
        this.ttsPlayer = ttsPlayer;
        prefetchImages(message);
    }

    private void prefetchImages(Message message) {
        for (Part part : message.getParts()) {
            if (part.type == Message.Type.Image) {
                final String resourcePath = part.value;
                if (part.value != Message.NoImage) {
                    imageFetcher.add(resourcePath, new Callable<byte[]>() {
                        @Override
                        public byte[] call() throws Exception {
                            return getImageBytes(resourcePath);
                        }

                        private byte[] getImageBytes(String path)
                                throws IOException {
                            InputStream resource = null;
                            byte[] imageBytes = null;
                            try {
                                resource = RenderMessage.this.resources
                                        .getResource(path);
                                imageBytes = convertInputStreamToByte(resource);
                            } catch (IOException e) {
                                if (!RenderMessage.this.teaseLib
                                        .getConfigSetting(
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

                        private byte[] convertInputStreamToByte(InputStream is)
                                throws IOException {
                            byte[] buffer = new byte[8192];
                            int bytesRead;
                            ByteArrayOutputStream output = new ByteArrayOutputStream();
                            while ((bytesRead = is.read(buffer)) != -1) {
                                output.write(buffer, 0, bytesRead);
                            }
                            return output.toByteArray();
                        }
                    });
                }
            }
        }
        imageFetcher.fetch();
    }

    public Message getMessage() {
        return message;
    }

    @Override
    public void renderMedia() throws InterruptedException {
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
                lastSection.add(new Message.Part(part.type, part.value));
            }
            if (part.type == Message.Type.Text) {
                afterText = true;
            }
        }
        return lastSection;
    }

    private void renderMessage(Message message, boolean speakText) {
        if (message.isEmpty()) {
            // // Show image but no text
            show(null, Mood.Neutral);
        } else {
            MessageTextAccumulator accumulatedText = new MessageTextAccumulator();
            String mood = Mood.Neutral;
            // Process message parts
            for (Iterator<Part> it = message.iterator(); it.hasNext();) {
                Part part = it.next();
                boolean lastParagraph = !it.hasNext();
                if (!logSpecialMessageTypes.contains(part.type)) {
                    teaseLib.transcript
                            .info("" + part.type.name() + " = " + part.value);
                }
                logger.info(part.type.toString() + ": " + part.value);
                // TODO Works only if the last part is a text part
                mood = renderMessagePart(message, part, accumulatedText, mood,
                        speakText, lastParagraph);
                if (task.isCancelled()) {
                    break;
                }
                if (lastParagraph) {
                    break;
                }
            }
            // Finished all parts
            completeSpeech(true);
            allCompleted();
        }
    }

    private String renderMessagePart(Message message, Part part,
            MessageTextAccumulator accumulatedText, String mood,
            boolean speakText, boolean lastParagraph) {
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
                        getParagraphPause(accumulatedText, lastParagraph),
                        resources, teaseLib);
            }
        } else if (part.type == Message.Type.DesktopItem) {
            // Finish the current text part
            try {
                final RenderDesktopItem renderDesktopItem = new RenderDesktopItem(
                        resources.unpackEnclosingFolder(part.value), teaseLib);
                completeSpeech(lastParagraph);
                renderDesktopItem.render();
            } catch (IOException e) {
                logger.error(e.getMessage(), e);
                accumulatedText
                        .add(new Part(Message.Type.Text, e.getMessage()));
                show(accumulatedText.toString(), mood);
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
            accumulatedText.add(part);
        } else { // (part.type == Message.Type.Text)
            accumulatedText.add(part);
            show(accumulatedText.toString(), mood);
            if (ttsPlayer != null && speechRenderer == null) {
                if (speakText) {
                    speechRenderer = new RenderTTSSpeech(ttsPlayer,
                            message.actor, part.value, mood,
                            getParagraphPause(accumulatedText, lastParagraph),
                            teaseLib);
                }
            }
            teaseLib.transcript.info(">> " + part.value);
            if (speechRenderer != null) {
                speak();
            }
        }
        return mood;
    }

    private void speak() {
        synchronized (interuptableAudio) {
            // Play sound, wait until finished
            speechRenderer.render();
            // Automatically interrupt
            interuptableAudio.add(speechRenderer);
        }
        speechRendererInProgress = speechRenderer;
        speechRenderer = null;
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

    private void show(String text, String mood) {
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
        if (path != null) {
            try {
                imageBytes = imageFetcher.get(path);
            } catch (Exception e) {
                text = text + "\n\n" + e.getClass() + ": " + e.getMessage()
                        + "\n";
                logger.error(e.getMessage(), e);
            } finally {
                synchronized (imageFetcher) {
                    if (!imageFetcher.isEmpty()) {
                        imageFetcher.fetch();
                    }
                }
            }
        }
        // speech in progress never refers to the last paragraph
        completeSpeech(false);
        teaseLib.host.show(imageBytes, text);
        // First message shown - start part completed
        startCompleted();
    }

    private static long getParagraphPause(
            MessageTextAccumulator accumulatedText, boolean lastParagraph) {
        if (lastParagraph) {
            return DELAY_AT_END_OF_MESSAGE;
        } else if (accumulatedText.canAppend()) {
            // TODO pause is imperfect with M$ TTS
            // - different from speaking the whole sentence at once
            return 0;
        } else {
            return DELAY_BETWEEN_PARAGRAPHS;
        }
    }

    private void doKeyword(Part part) {
        String keyword = part.value;
        if (keyword == Message.ActorImage) {
            // Image has to be resolved in preprocessMessage
            throw new IllegalStateException(keyword);
        } else if (keyword == Message.NoImage) {
            // No image
            displayImage = Message.NoImage;
        } else if (keyword == Message.ShowChoices) {
            // Complete the mandatory part of the message
            if (speechRendererInProgress != null) {
                // Finish speech first
                speechRendererInProgress.completeMandatory();
            }
            mandatoryCompleted();
        } else if (keyword == Message.AwaitSoundCompletion) {
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
            teaseLib.sleep(DELAY_AT_END_OF_MESSAGE, TimeUnit.MILLISECONDS);
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
                teaseLib.sleep(DELAY_AT_END_OF_MESSAGE, TimeUnit.MILLISECONDS);
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

    @Override
    public String toString() {
        long delay = 0;
        Message.Parts paragraphs = message.getParts();
        for (Iterator<Part> it = paragraphs.iterator(); it.hasNext();) {
            Part paragraph = it.next();
            delay += TextToSpeech.getEstimatedSpeechDuration(paragraph.value);
            if (it.hasNext()) {
                delay += DELAY_BETWEEN_PARAGRAPHS;
            } else {
                delay += DELAY_AT_END_OF_MESSAGE;
            }
        }
        String messageText = message.toPrerecordedSpeechHashString()
                .replace("\n", " ");
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
