package teaselib.core.media;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import teaselib.Actor;
import teaselib.Config;
import teaselib.Message;
import teaselib.Message.Type;
import teaselib.MessageParts;
import teaselib.Mood;
import teaselib.MessagePart;
import teaselib.Replay;
import teaselib.Replay.Position;
import teaselib.core.ResourceLoader;
import teaselib.core.TeaseLib;
import teaselib.core.texttospeech.TextToSpeech;
import teaselib.core.texttospeech.TextToSpeechPlayer;
import teaselib.core.util.ExceptionUtil;
import teaselib.core.util.PrefetchImage;
import teaselib.core.util.Prefetcher;
import teaselib.util.Interval;

public class RenderMessage extends MediaRendererThread implements ReplayableMediaRenderer {
    private static final Logger logger = LoggerFactory.getLogger(RenderMessage.class);

    private static final long DELAY_BETWEEN_PARAGRAPHS = 500;
    private static final long DELAY_AT_END_OF_MESSAGE = 2000;

    static final Set<Message.Type> ManuallyLoggedMessageTypes = new HashSet<>(
            Arrays.asList(Message.Type.Text, Message.Type.Image, Message.Type.Mood, Message.Type.Speech));

    private final ResourceLoader resources;
    private final List<Message> messages;
    private final Optional<TextToSpeechPlayer> ttsPlayer;

    private final Prefetcher<byte[]> imageFetcher = new Prefetcher<>();

    private MediaRendererThread speechRenderer = null;
    private MediaRendererThread speechRendererInProgress = null;
    private RenderSound soundRenderer = null;
    private String displayImage = null;

    MessageTextAccumulator accumulatedText;
    int currentMessage;
    Message lastSection;

    private final Set<MediaRendererThread> interruptibleAudio = new HashSet<>();

    public RenderMessage(TeaseLib teaseLib, ResourceLoader resources, Optional<TextToSpeechPlayer> ttsPlayer,
            Message... messages) {
        this(teaseLib, resources, ttsPlayer, Arrays.asList(messages));
    }

    public RenderMessage(TeaseLib teaseLib, ResourceLoader resources, Optional<TextToSpeechPlayer> ttsPlayer,
            List<Message> messages) {
        super(teaseLib);

        if (messages == null) {
            throw new NullPointerException();
        }

        this.resources = resources;
        this.messages = messages;
        this.ttsPlayer = ttsPlayer;

        prefetchImages(this.messages);

        accumulatedText = new MessageTextAccumulator();
        currentMessage = 0;
        lastSection = getLastSection(getLastMessage());
    }

    public void append(Message message) {
        synchronized (messages) {
            messages.add(message);
            lastSection = getLastSection(message);
        }
    }

    private Message getLastMessage() {
        return this.messages.get(this.messages.size() - 1);
    }

    private void prefetchImages(List<Message> messages) {
        for (Message message : messages) {
            prefetchImages(message);
        }
    }

    private void prefetchImages(Message message) {
        for (MessagePart part : message.getParts()) {
            if (part.type == Message.Type.Image) {
                final String resourcePath = part.value;
                if (part.value != Message.NoImage) {
                    imageFetcher.add(resourcePath, new PrefetchImage(resourcePath, resources, teaseLib.config));
                }
            }
        }
        imageFetcher.fetch();
    }

    @Override
    public void renderMedia() throws IOException, InterruptedException {
        if (messages.isEmpty()) {
            throw new IllegalStateException();
        }
        if (messages.get(0).isEmpty()) {
            // Show image but no text
            show(null, messages.get(0).actor, Mood.Neutral);
            mandatoryCompleted();
        } else {
            if (replayPosition == Replay.Position.FromStart) {
                accumulatedText = new MessageTextAccumulator();
                currentMessage = 0;
                render(messages);
            } else if (replayPosition == Replay.Position.FromMandatory) {
                // TODO remember accumulated text so that all but the last section
                // is displayed, rendered, but the text not added again
                // TODO Remove all but last speech and delay parts
                renderMessage(getMandatory(messages));
            } else if (replayPosition == Replay.Position.End) {
                // TODO Remove all speech and delay parts
                renderMessage(getEnd(messages));
            } else {
                throw new IllegalStateException(replayPosition.toString());
            }
        }
    }

    private Message getMandatory(List<Message> messages) {
        return getLastSection(getLastMessage());
    }

    private Message getEnd(List<Message> messages) {
        // TODO Return just the text, not the speech
        return getLastSection(getLastMessage());
    }

    @Override
    public void replay(Position replayPosition) {
        super.replay(replayPosition);
    }

    private static Message getLastSection(Message message) {
        int index = findLastTextElement(message);

        if (index < 0) {
            return message;
        }

        index = findStartOfHeader(message, index);
        return copyTextHeader(message, index);
    }

    private static int findStartOfHeader(Message message, int index) {
        MessageParts parts = message.getParts();
        while (index-- > 0) {
            Type type = parts.get(index).type;
            if (type == Message.Type.Text && type == Message.Type.Delay) {
                // Start the last section after the second last text part
                index++;
                break;
            }
        }
        return index;
    }

    private static int findLastTextElement(Message message) {
        MessageParts parts = message.getParts();
        int index = parts.size();
        while (index-- > 0) {
            Type type = parts.get(index).type;
            if (type == Message.Type.Text) {
                break;
            }
        }
        return index;
    }

    private static Message copyTextHeader(Message message, int index) {
        if (index < 0) {
            index = 0;
        }

        Message lastSection = new Message(message.actor);
        boolean afterText = false;
        MessageParts parts = message.getParts();
        for (int i = index; i < parts.size(); i++) {
            MessagePart part = parts.get(i);
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

    private void render(List<Message> messages) throws IOException, InterruptedException {
        for (; currentMessage < messages.size(); currentMessage++) {
            renderMessage(messages.get(currentMessage));
        }
    }

    private void renderMessage(Message message) throws IOException, InterruptedException {
        String mood = Mood.Neutral;
        // Process message parts
        for (Iterator<MessagePart> it = message.iterator(); it.hasNext();) {
            MessagePart part = it.next();
            boolean lastParagraph = lastSection.getParts().contains(part);
            boolean lastPart = !it.hasNext();
            if (!ManuallyLoggedMessageTypes.contains(part.type)) {
                teaseLib.transcript.info("" + part.type.name() + " = " + part.value);
            }
            logger.info(part.type.toString() + ": " + part.value);

            mood = renderMessagePart(part, accumulatedText, message.actor, mood, lastParagraph);
            if (part.type == Message.Type.Text) {
                show(part.value, accumulatedText, message.actor, mood);
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

    private String renderMessagePart(MessagePart part, MessageTextAccumulator accumulatedText, Actor actor, String mood,
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
            long paragraphPause = getParagraphPause(accumulatedText, lastParagraph);
            if (Message.Type.isSound(part.value)) {
                speechRenderer = new RenderPrerecordedSpeech(part.value, paragraphPause, resources, teaseLib);
            } else if (TextToSpeechPlayer.isSimulatedSpeech(part.value)) {
                speechRenderer = new RenderSpeechDelay(TextToSpeechPlayer.getSimulatedSpeechText(part.value),
                        paragraphPause, teaseLib);
            } else if (isSpeechOutputEnabled() && ttsPlayer.isPresent()) {
                speechRenderer = new RenderTTSSpeech(ttsPlayer.get(), actor, part.value, mood, paragraphPause,
                        teaseLib);
            } else {
                speechRenderer = new RenderSpeechDelay(part.value, paragraphPause, teaseLib);
            }
        } else if (part.type == Message.Type.DesktopItem) {
            if (isInstructionalImageOutputEnabled()) {
                try {
                    RenderDesktopItem renderDesktopItem = new RenderDesktopItem(
                            resources.unpackEnclosingFolder(part.value), teaseLib);
                    completeCurrentParagraph(lastParagraph);
                    renderDesktopItem.render();
                } catch (IOException e) {
                    logger.error(e.getMessage(), e);
                    accumulatedText.add(new MessagePart(Message.Type.Text, e.getMessage()));
                    show(accumulatedText.toString(), actor, mood);
                    throw e;
                }
            }
        } else if (part.type == Message.Type.Mood) {
            mood = part.value;
        } else if (part.type == Message.Type.Keyword) {
            doKeyword(part);
        } else if (part.type == Message.Type.Delay) {
            doDelay(part, lastParagraph);
        } else if (part.type == Message.Type.Item) {
            accumulatedText.add(part);
        } else if (part.type == Message.Type.Text) {
            accumulatedText.add(part);
        } else {
            throw new UnsupportedOperationException(part.type + "=" + part.value);
        }
        return mood;
    }

    private void show(String text, MessageTextAccumulator accumulatedText, Actor actor, String mood)
            throws IOException, InterruptedException {
        teaseLib.transcript.info(text);
        show(accumulatedText.toString(), actor, mood);
        if (speechRenderer != null && !isDoneOrCancelled()) {
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

    private void completeCurrentParagraph(boolean lastParagraph) {
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

    private void show(String text, Actor actor, String mood) throws IOException, InterruptedException {
        logMoodToTranscript(actor, mood);
        logImageToTranscript(actor);
        show(text);
    }

    private void logMoodToTranscript(Actor actor, String mood) {
        if (actor.images.contains(displayImage) && mood != Mood.Neutral) {
            teaseLib.transcript.info("mood = " + mood);
        }
    }

    private void logImageToTranscript(Actor actor) {
        if (displayImage == Message.NoImage) {
            if (!Boolean.parseBoolean(teaseLib.config.get(Config.Debug.StopOnAssetNotFound))) {
                teaseLib.transcript.info(Message.NoImage);
            }
        } else {
            if (actor.images.contains(displayImage)) {
                teaseLib.transcript.debug("image = '" + displayImage + "'");
            } else {
                teaseLib.transcript.info("image = '" + displayImage + "'");
            }
        }
    }

    private void show(String text) throws IOException, InterruptedException {
        completeCurrentParagraph(false);

        if (!isDoneOrCancelled()) {
            teaseLib.host.show(getImageBytes(), text);
            // First message shown - start part completed
            startCompleted();
        }
    }

    private byte[] getImageBytes() throws InterruptedException, IOException {
        if (displayImage != null && displayImage != Message.NoImage) {
            try {
                return imageFetcher.get(displayImage);
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
        return null;
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

    private void doKeyword(MessagePart part) {
        String keyword = part.value;
        if (keyword == Message.ActorImage) {
            throw new IllegalStateException(keyword + " must be resolved in pre-parse");
        } else if (keyword == Message.NoImage) {
            throw new IllegalStateException(keyword + " must be resolved in pre-parse");
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

    private void doDelay(MessagePart part, boolean lastParagraph) {
        completeCurrentParagraph(false);

        String args = part.value;
        if (args.isEmpty()) {
            teaseLib.sleep(DELAY_AT_END_OF_MESSAGE, TimeUnit.MILLISECONDS);
        } else {
            Interval delay = getDelay(part.value);
            teaseLib.sleep(teaseLib.random(delay.start, delay.end), TimeUnit.MILLISECONDS);
        }

        if (lastParagraph) {
            mandatoryCompleted();
        }
    }

    private static Interval getDelay(String args) {
        String[] argv = args.split(" ");
        if (argv.length == 1) {
            int delay = (int) (Double.parseDouble(args) * 1000);
            return new Interval(delay, delay);
        } else {
            double start = Double.parseDouble(argv[0]) * 1000;
            double end = Double.parseDouble(argv[1]) * 1000;
            return new Interval((int) start, (int) end);
        }
    }

    @Override
    public String toString() {
        long delay = 0;
        MessageTextAccumulator accumulatedText = new MessageTextAccumulator();
        for (Message message : messages) {
            MessageParts paragraphs = message.getParts();
            for (Iterator<MessagePart> it = paragraphs.iterator(); it.hasNext();) {
                MessagePart part = it.next();
                accumulatedText.add(part);
                if (part.type == Type.Text) {
                    delay += TextToSpeech.getEstimatedSpeechDuration(part.value);
                    delay += getParagraphPause(accumulatedText, !it.hasNext());
                } else if (part.type == Type.Delay) {
                    delay += getDelay(part.value).start;
                }
            }
        }
        String messageText = accumulatedText.toString().replace("\n", " ");
        int length = 40;
        return "Estimated delay=" + String.format("%.2f", (double) delay / 1000) + " Message='"
                + (messageText.length() > length ? messageText.substring(0, length) + "..." : messageText + "'");
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
