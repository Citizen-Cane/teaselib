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

import teaselib.AbstractMessage;
import teaselib.Actor;
import teaselib.Config;
import teaselib.Message;
import teaselib.Message.Type;
import teaselib.MessagePart;
import teaselib.Mood;
import teaselib.Replay.Position;
import teaselib.Replay.Replayable;
import teaselib.core.ResourceLoader;
import teaselib.core.TeaseLib;
import teaselib.core.texttospeech.TextToSpeech;
import teaselib.core.texttospeech.TextToSpeechPlayer;
import teaselib.core.util.ExceptionUtil;
import teaselib.core.util.PrefetchImage;
import teaselib.core.util.Prefetcher;
import teaselib.util.Interval;

/**
 * Play all parts:
 * <li>text is displayed immediately.
 * <li>images are displayed with the next text element
 * <li>background sounds are started immediately, but there can be only one background sound at a time
 * <li>keywords are rendered immediately
 * <li>time-spanned items (sound, speech, delay) are started in background (so other items can be rendered in parallel),
 * but only one time-spanned item at a time can be rendered
 * 
 * @author Citizen-Cane
 *
 */
public class RenderMessage extends MediaRendererThread implements ReplayableMediaRenderer {
    private static final Logger logger = LoggerFactory.getLogger(RenderMessage.class);

    private static final Set<Message.Type> ManuallyLoggedMessageTypes = new HashSet<>(
            Arrays.asList(Message.Type.Text, Message.Type.Image, Message.Type.Mood, Message.Type.Speech));

    private static final Set<Type> SoundTypes = new HashSet<>(
            Arrays.asList(Type.Speech, Type.Sound, Type.BackgroundSound));

    private final Prefetcher<byte[]> imageFetcher = new Prefetcher<>();

    private final ResourceLoader resources;
    private final TextToSpeechPlayer ttsPlayer;
    private final List<Message> messages;

    private MessageTextAccumulator accumulatedText;
    private int currentMessage;
    private Message lastSection;

    // private MediaRendererThread speechRenderer = null;
    private RenderSound backgroundSoundRenderer = null;

    private String displayImage = null;
    private MediaRenderer.Threaded currentRenderer = null;

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
        this.ttsPlayer = ttsPlayer.isPresent() ? ttsPlayer.get() : null;
        this.messages = messages;

        this.accumulatedText = new MessageTextAccumulator();
        this.currentMessage = 0;
        this.lastSection = RenderedMessage.getLastSection(getLastMessage());

        prefetchImages(this.messages);
    }

    public void append(Message message) {
        synchronized (messages) {
            prefetchImages(message);
            messages.add(message);
            lastSection = RenderedMessage.getLastSection(message);
        }

        if (hasCompletedMandatory()) {
            completeAll();
            replay(Position.FromCurrentPosition);
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
        for (MessagePart part : message) {
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
            show(null, messages.get(0).actor, Mood.Neutral);
            mandatoryCompleted();
            allCompleted();
        } else {
            if (replayPosition == Position.FromStart) {
                accumulatedText = new MessageTextAccumulator();
                currentMessage = 0;
                renderMessages();
            } else if (replayPosition == Position.FromCurrentPosition) {
                Replayable replay;
                synchronized (messages) {
                    if (currentMessage < messages.size()) {
                        replay = this::renderMessages;
                    } else {
                        replay = () -> renderMessage(getEnd());
                    }
                }
                replay.run();
            } else if (replayPosition == Position.FromMandatory) {
                // TODO remember accumulated text so that all but the last section
                // is displayed, rendered, but the text not added again
                // TODO Remove all but last speech and delay parts
                renderMessage(getMandatory());
                allCompleted();
            } else if (replayPosition == Position.End) {
                renderMessage(getEnd());
                allCompleted();
            } else {
                throw new IllegalStateException(replayPosition.toString());
            }
        }
    }

    private Message getMandatory() {
        return RenderedMessage.getLastSection(getLastMessage());
    }

    private Message getEnd() {
        return stripAudio(RenderedMessage.getLastSection(getLastMessage()));
    }

    private Message stripAudio(Message message) {
        return message.stream().filter(part -> !SoundTypes.contains(part.type)).collect(message.collector());
    }

    private void renderMessages() throws IOException, InterruptedException {
        while (true) {
            Message message;
            synchronized (messages) {
                if (currentMessage >= messages.size()) {
                    break;
                }
                message = messages.get(currentMessage);
            }
            renderMessage(message);
            currentMessage++;
        }

        allCompleted();
    }

    @Override
    protected void allCompleted() {
        completeSectionMandatory();
        mandatoryCompleted();
        completeSectionAll();

        super.allCompleted();
    }

    /**
     * 
     * @param message
     * @throws IOException
     * @throws InterruptedException
     */
    private void renderMessage(Message message) throws IOException, InterruptedException {
        String mood = Mood.Neutral;
        for (Iterator<MessagePart> it = message.iterator(); it.hasNext();) {
            MessagePart part = it.next();
            boolean lastPart = !it.hasNext();
            if (!ManuallyLoggedMessageTypes.contains(part.type)) {
                teaseLib.transcript.info("" + part.type.name() + " = " + part.value);
            }
            logger.info("{}={}", part.type, part.value);

            if (part.type == Message.Type.Mood) {
                mood = part.value;
            } else {
                renderMessagePart(part, accumulatedText, message.actor, mood);
            }

            if (part.type == Message.Type.Text) {
                completeSectionMandatory();
                completeSectionAll();
                show(part.value, accumulatedText, message.actor, mood);
            } else if (lastPart) {
                completeSectionMandatory();
                completeSectionAll();
                show(accumulatedText.toString());
            }

            if (isDoneOrCancelled()) {
                break;
            }
        }
    }

    private boolean isLastParagraph(MessagePart part) {
        return lastSection.contains(part);
    }

    private void renderMessagePart(MessagePart part, MessageTextAccumulator accumulatedText, Actor actor, String mood)
            throws IOException, InterruptedException {
        if (part.type == Message.Type.Image) {
            displayImage = part.value;
        } else if (part.type == Message.Type.BackgroundSound) {
            playSoundAsynchronous(part);
            // use awaitSoundCompletion keyword to wait for background sound completion
        } else if (part.type == Message.Type.Sound) {
            playSoundAndWait(part);
        } else if (part.type == Message.Type.Speech) {
            playSpeechAndWait(part, actor, mood);
        } else if (part.type == Message.Type.DesktopItem) {
            if (isInstructionalImageOutputEnabled()) {
                try {
                    showDesktopItem(part);
                } catch (IOException e) {
                    showDesktopItemError(accumulatedText, actor, mood, e);
                    throw e;
                }
            }
        } else if (part.type == Message.Type.Keyword) {
            doKeyword(part);
        } else if (part.type == Message.Type.Delay) {
            doDelay(part);
        } else if (part.type == Message.Type.Item) {
            accumulatedText.add(part);
        } else if (part.type == Message.Type.Text) {
            accumulatedText.add(part);
        } else {
            throw new UnsupportedOperationException(part.type + "=" + part.value);
        }
    }

    private void renderTimeSpannedPart(MediaRenderer.Threaded renderer) throws IOException {
        if (this.currentRenderer != null) {
            this.currentRenderer.completeMandatory();
        }
        this.currentRenderer = renderer;
        this.currentRenderer.render();
    }

    private void showDesktopItem(MessagePart part) throws IOException {
        RenderDesktopItem renderDesktopItem = new RenderDesktopItem(resources.unpackEnclosingFolder(part.value),
                teaseLib);
        completeSectionAll();
        renderDesktopItem.render();
    }

    private void showDesktopItemError(MessageTextAccumulator accumulatedText, Actor actor, String mood, IOException e)
            throws IOException, InterruptedException {
        accumulatedText.add(new MessagePart(Message.Type.Text, e.getMessage()));
        completeSectionAll();
        show(accumulatedText.toString(), actor, mood);
    }

    private void playSpeechAndWait(MessagePart part, Actor actor, String mood) throws IOException {
        if (Message.Type.isSound(part.value)) {
            renderTimeSpannedPart(new RenderPrerecordedSpeech(part.value, resources, teaseLib));
        } else if (TextToSpeechPlayer.isSimulatedSpeech(part.value)) {
            renderTimeSpannedPart(
                    new RenderSpeechDelay(TextToSpeechPlayer.getSimulatedSpeechText(part.value), teaseLib));
        } else if (isSpeechOutputEnabled() && ttsPlayer != null) {
            renderTimeSpannedPart(new RenderTTSSpeech(ttsPlayer, actor, part.value, mood, teaseLib));
        } else {
            renderTimeSpannedPart(new RenderSpeechDelay(part.value, teaseLib));
        }
    }

    private void playSoundAndWait(MessagePart part) throws IOException {

        if (isSoundOutputEnabled()) {
            renderTimeSpannedPart(new RenderSound(resources, part.value, teaseLib));
        }
    }

    private void playSoundAsynchronous(MessagePart part) {
        completeSectionMandatory();

        if (isSoundOutputEnabled()) {
            if (backgroundSoundRenderer != null) {
                backgroundSoundRenderer.interrupt();
            }
            backgroundSoundRenderer = new RenderSound(resources, part.value, teaseLib);
            backgroundSoundRenderer.render();
        }
    }

    private void show(String text, MessageTextAccumulator accumulatedText, Actor actor, String mood)
            throws IOException, InterruptedException {
        teaseLib.transcript.info(text);
        show(accumulatedText.toString(), actor, mood);
    }

    private void completeSectionMandatory() {
        if (currentRenderer != null) {
            currentRenderer.completeMandatory();
        }
    }

    private void completeSectionAll() {
        if (currentRenderer != null) {
            currentRenderer.completeAll();
            currentRenderer = null;
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
        return new byte[] {};
    }

    private void doKeyword(MessagePart part) {
        String keyword = part.value;
        if (keyword == Message.ActorImage) {
            throw new IllegalStateException(keyword + " must be resolved in pre-parse");
        } else if (keyword == Message.NoImage) {
            throw new IllegalStateException(keyword + " must be resolved in pre-parse");
        } else if (keyword == Message.ShowChoices) {
            completeSectionMandatory();
            mandatoryCompleted();
        } else if (keyword == Message.AwaitSoundCompletion) {
            backgroundSoundRenderer.completeAll();
            backgroundSoundRenderer = null;
        } else {
            throw new UnsupportedOperationException(keyword);
        }
    }

    private void doDelay(MessagePart part) {
        completeSectionMandatory();

        String args = part.value;
        if (args.isEmpty()) {
            completeSectionAll();
        } else {
            Interval delay = getDelay(part.value);
            teaseLib.sleep(teaseLib.random(delay.start, delay.end), TimeUnit.MILLISECONDS);
        }

        if (isLastParagraph(part)) {
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
        MessageTextAccumulator text = new MessageTextAccumulator();
        for (Message message : messages) {
            AbstractMessage paragraphs = message;
            for (Iterator<MessagePart> it = paragraphs.iterator(); it.hasNext();) {
                MessagePart part = it.next();
                text.add(part);
                if (part.type == Type.Text) {
                    delay += TextToSpeech.getEstimatedSpeechDuration(part.value);
                } else if (part.type == Type.Delay) {
                    delay += getDelay(part.value).start;
                }
            }
        }
        String messageText = text.toString().replace("\n", " ");
        int length = 40;
        return "Estimated delay=" + String.format("%.2f", (double) delay / 1000) + " Message='"
                + (messageText.length() > length ? messageText.substring(0, length) + "..." : messageText + "'");
    }

    @Override
    public void interrupt() {
        if (currentRenderer != null) {
            currentRenderer.interrupt();
        }

        if (backgroundSoundRenderer != null) {
            backgroundSoundRenderer.interrupt();
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

    public Optional<TextToSpeechPlayer> getTextToSpeech() {
        return Optional.ofNullable(ttsPlayer);
    }
}
