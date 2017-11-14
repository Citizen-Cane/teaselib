package teaselib.core.media;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import teaselib.Actor;
import teaselib.Config;
import teaselib.Message;
import teaselib.Message.Part;
import teaselib.Mood;
import teaselib.Replay;
import teaselib.core.Prefetcher;
import teaselib.core.ResourceLoader;
import teaselib.core.TeaseLib;
import teaselib.core.texttospeech.TextToSpeech;
import teaselib.core.texttospeech.TextToSpeechPlayer;
import teaselib.core.util.ExceptionUtil;

public class RenderMessage extends MediaRendererThread {
    private static final Logger logger = LoggerFactory.getLogger(RenderMessage.class);

    private static final long DELAY_BETWEEN_PARAGRAPHS = 500;
    private static final long DELAY_AT_END_OF_MESSAGE = 2000;

    static final Set<Message.Type> ManuallyLoggedMessageTypes = new HashSet<>(
            Arrays.asList(Message.Type.Text, Message.Type.Image, Message.Type.Mood, Message.Type.Speech));

    private final ResourceLoader resources;
    private final Message message;
    private final Optional<TextToSpeechPlayer> ttsPlayer;
    MediaRendererThread speechRenderer = null;
    MediaRendererThread speechRendererInProgress = null;
    RenderSound soundRenderer = null;

    private String displayImage = null;
    private final Prefetcher<byte[]> imageFetcher = new Prefetcher<>();

    private final Set<MediaRendererThread> interruptibleAudio = new HashSet<>();

    public RenderMessage(ResourceLoader resources, Message message, Optional<TextToSpeechPlayer> ttsPlayer,
            TeaseLib teaseLib) {
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

                        private byte[] getImageBytes(String path) throws IOException {
                            InputStream resource = null;
                            byte[] imageBytes = null;
                            try {
                                resource = RenderMessage.this.resources.getResource(path);
                                imageBytes = convertInputStreamToByte(resource);
                            } catch (IOException e) {
                                handleIOException(ExceptionUtil.reduce(e));
                            } finally {
                                if (resource != null) {
                                    resource.close();
                                }
                            }
                            return imageBytes;
                        }

                        private byte[] convertInputStreamToByte(InputStream is) throws IOException {
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
    public void renderMedia() throws IOException, InterruptedException {
        if (replayPosition == Replay.Position.FromStart) {
            renderMessage(message, true);
        } else {
            Message lastSection = getLastSection(message);
            if (replayPosition == Replay.Position.FromMandatory) {
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
                lastSection.add(part);
            }
            if (part.type == Message.Type.Text) {
                afterText = true;
            }
        }
        return lastSection;
    }

    private void renderMessage(Message message, boolean speakText) throws IOException, InterruptedException {
        try {
            if (message.isEmpty()) {
                // Show image but no text
                show(null, Mood.Neutral);
                mandatoryCompleted();
            } else {
                MessageTextAccumulator accumulatedText = new MessageTextAccumulator();
                String mood = Mood.Neutral;
                Message lastSection = getLastSection(message);
                // Process message parts
                for (Iterator<Part> it = message.iterator(); it.hasNext();) {
                    Part part = it.next();
                    boolean lastParagraph = lastSection.getParts().contains(part);
                    boolean lastPart = !it.hasNext();
                    if (!ManuallyLoggedMessageTypes.contains(part.type)) {
                        teaseLib.transcript.info("" + part.type.name() + " = " + part.value);
                    }
                    logger.info(part.type.toString() + ": " + part.value);

                    mood = renderMessagePart(part, accumulatedText, mood, speakText, lastParagraph);
                    if (part.type == Message.Type.Text) {
                        show(message.actor, part, accumulatedText, mood, speakText, lastParagraph);
                    } else if (lastPart) {
                        show(accumulatedText.toString());
                    }

                    if (isDoneOrCancelled()) {
                        break;
                    }
                }
                completeCurrentParagraph(true);
                allCompleted();
            }
        } finally {
        }
    }

    private String renderMessagePart(Part part, MessageTextAccumulator accumulatedText, String mood, boolean speakText,
            boolean lastParagraph) throws IOException, InterruptedException {
        if (part.type == Message.Type.Image) {
            displayImage = part.value;
        } else if (part.type == Message.Type.BackgroundSound) {
            // Play sound, continue message execution
            completeCurrentParagraph(lastParagraph);
            if (isSoundOutputEnabled()) {
                synchronized (interruptibleAudio) {
                    soundRenderer = new RenderSound(resources, part.value, teaseLib);
                    soundRenderer.render();
                    interruptibleAudio.add(soundRenderer);
                }
            }
            // use awaitSoundCompletion keyword to wait for sound completion
        } else if (part.type == Message.Type.Sound) {
            // Play sound, wait until finished
            completeCurrentParagraph(lastParagraph);
            if (isSoundOutputEnabled()) {
                RenderSound sound = new RenderSound(resources, part.value, teaseLib);
                synchronized (interruptibleAudio) {
                    sound.render();
                    interruptibleAudio.add(sound);
                }
                sound.completeAll();
                synchronized (interruptibleAudio) {
                    interruptibleAudio.remove(sound);
                }
            }
        } else if (part.type == Message.Type.Speech) {
            if (speakText) {
                long paragraphPause = getParagraphPause(accumulatedText, lastParagraph);
                speechRenderer = isSpeechOutputEnabled()
                        ? new RenderPrerecordedSpeech(part.value, paragraphPause, resources, teaseLib)
                        : new RenderSpeechDelay(paragraphPause, teaseLib, part.value);
            }
        } else if (part.type == Message.Type.DesktopItem) {
            if (isInstructionalImageOutputEnabled()) {
                try {
                    final RenderDesktopItem renderDesktopItem = new RenderDesktopItem(
                            resources.unpackEnclosingFolder(part.value), teaseLib);
                    completeCurrentParagraph(lastParagraph);
                    renderDesktopItem.render();
                } catch (IOException e) {
                    logger.error(e.getMessage(), e);
                    accumulatedText.add(new Part(Message.Type.Text, e.getMessage()));
                    show(accumulatedText.toString(), mood);
                    throw e;
                }
            }
        } else if (part.type == Message.Type.Mood) {
            mood = part.value;
        } else if (part.type == Message.Type.Keyword) {
            doKeyword(part);
        } else if (part.type == Message.Type.Delay) {
            completeCurrentParagraph(lastParagraph);
            doDelay(part);
        } else if (part.type == Message.Type.Item) {
            accumulatedText.add(part);
        } else if (part.type == Message.Type.Text) {
            accumulatedText.add(part);
        } else {
            throw new UnsupportedOperationException(part.type + "=" + part.value);
        }
        return mood;
    }

    private void show(Actor actor, Part part, MessageTextAccumulator accumulatedText, String mood, boolean speakText,
            boolean lastParagraph) throws IOException, InterruptedException {
        if (speechRenderer == null) {
            long paragraphPause = getParagraphPause(accumulatedText, lastParagraph);
            speechRenderer = speakText && ttsPlayer.isPresent() && isSpeechOutputEnabled()
                    ? new RenderTTSSpeech(ttsPlayer.get(), actor, part.value, mood, paragraphPause, teaseLib)
                    : new RenderSpeechDelay(paragraphPause, teaseLib, part.value);
        }

        teaseLib.transcript.info(part.value);
        show(accumulatedText.toString(), mood);

        if (!isDoneOrCancelled()) {
            speak();
        }
    }

    private void speak() {
        synchronized (interruptibleAudio) {
            speechRenderer.render();
            interruptibleAudio.add(speechRenderer);
            speechRendererInProgress = speechRenderer;
        }
        speechRenderer = null;
    }

    private void completeCurrentParagraph(final boolean lastParagraph) {
        if (speechRendererInProgress != null) {
            speechRendererInProgress.completeMandatory();
        }
        if (lastParagraph) {
            mandatoryCompleted();
        }
        if (speechRendererInProgress != null) {
            speechRendererInProgress.completeAll();
            synchronized (interruptibleAudio) {
                interruptibleAudio.remove(speechRendererInProgress);
                speechRendererInProgress = null;
            }
        }
    }

    private void show(String text, String mood) throws IOException, InterruptedException {
        if (message.actor.images.contains(displayImage) && mood != Mood.Neutral) {
            teaseLib.transcript.info("mood = " + mood);
        }

        show(text);
    }

    private void show(String text) throws IOException, InterruptedException {
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
            } catch (IOException e) {
                handleIOException(ExceptionUtil.reduce(e));
            } finally {
                synchronized (imageFetcher) {
                    if (!imageFetcher.isEmpty()) {
                        imageFetcher.fetch();
                    }
                }
            }
        }

        completeCurrentParagraph(false);

        if (!isDoneOrCancelled()) {
            teaseLib.host.show(imageBytes, text);
            // First message shown - start part completed
            startCompleted();
        }
    }

    private static long getParagraphPause(MessageTextAccumulator accumulatedText, boolean lastParagraph) {
        if (lastParagraph) {
            return DELAY_AT_END_OF_MESSAGE;
        } else if (accumulatedText.canAppend()) {
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
            synchronized (interruptibleAudio) {
                interruptibleAudio.remove(soundRenderer);
            }
        } else {
            throw new UnsupportedOperationException(keyword);
        }
    }

    private void doDelay(Part part) {
        String args = part.value;
        if (args.isEmpty()) {
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
                    teaseLib.sleep(teaseLib.random((int) delayFrom, (int) delayTo), TimeUnit.MILLISECONDS);
                }
            } catch (NumberFormatException ignore) {
                teaseLib.sleep(DELAY_AT_END_OF_MESSAGE, TimeUnit.MILLISECONDS);
            }
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
        String messageText = message.toPrerecordedSpeechHashString().replace("\n", " ");
        int length = 40;
        return "Estimated delay = " + String.format("%.2f", (double) delay / 1000) + " Message = "
                + (messageText.length() > length ? messageText.substring(0, length) + "..." : messageText);
    }

    @Override
    public void interrupt() {
        synchronized (interruptibleAudio) {
            for (MediaRendererThread sound : interruptibleAudio) {
                sound.interrupt();
            }
            interruptibleAudio.clear();
        }

        super.interrupt();
    }

    private boolean isSpeechOutputEnabled() {
        return Boolean.parseBoolean(teaseLib.config.get(Config.Render.Speech));
    }

    private boolean isSoundOutputEnabled() {
        return Boolean.parseBoolean(teaseLib.config.get(Config.Render.Sound));
    }

    private boolean isInstructionalImageOutputEnabled() {
        return Boolean.parseBoolean(teaseLib.config.get(Config.Render.InstructionalImages));
    }
}
