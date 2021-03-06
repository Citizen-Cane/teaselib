package teaselib.core.media;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.UnaryOperator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import teaselib.Actor;
import teaselib.Config;
import teaselib.Message;
import teaselib.Message.Type;
import teaselib.MessagePart;
import teaselib.Mood;
import teaselib.Replay.Position;
import teaselib.Replay.Replayable;
import teaselib.core.AbstractMessage;
import teaselib.core.ResourceLoader;
import teaselib.core.ScriptInterruptedException;
import teaselib.core.TeaseLib;
import teaselib.core.texttospeech.TextToSpeech;
import teaselib.core.texttospeech.TextToSpeechPlayer;
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

    private static final Set<Message.Type> ManuallyLoggedMessageTypes = new HashSet<>(Arrays.asList(Message.Type.Text,
            Message.Type.Image, Message.Type.Mood, Message.Type.Speech, Message.Type.Delay));

    private static final Set<Type> SoundTypes = new HashSet<>(
            Arrays.asList(Type.Speech, Type.Sound, Type.BackgroundSound));

    private static final double DELAY_AT_END_OF_MESSAGE = 2.0;

    private final AtomicReference<Thread> renderThread = new AtomicReference<>();
    private final Prefetcher<byte[]> imageFetcher;

    private final MediaRendererQueue renderQueue;

    private final ResourceLoader resources;
    private final TextToSpeechPlayer textToSpeechPlayer;
    private final Actor actor;
    private final List<RenderedMessage> messages;

    private MessageTextAccumulator accumulatedText;
    private int currentMessage;
    private AbstractMessage lastSection;

    private final Lock messageRenderingInProgressLock = new ReentrantLock();
    private final Condition messageRenderingModifierApplied = messageRenderingInProgressLock.newCondition();
    private final AtomicReference<UnaryOperator<List<RenderedMessage>>> messageModifier = new AtomicReference<>(null);

    private String displayImage = null;
    private MediaRenderer.Threaded currentRenderer = null;
    private RenderSound backgroundSoundRenderer = null;

    public RenderMessage(TeaseLib teaseLib, MediaRendererQueue renderQueue, ResourceLoader resources,
            Optional<TextToSpeechPlayer> ttsPlayer, Actor actor, List<RenderedMessage> messages) {
        super(teaseLib);
        if (messages == null) {
            throw new NullPointerException();
        }

        this.imageFetcher = new Prefetcher<>(renderQueue.getExecutorService());
        this.renderQueue = new MediaRendererQueue(renderQueue);
        this.actor = actor;
        this.resources = resources;
        this.textToSpeechPlayer = ttsPlayer.isPresent() ? ttsPlayer.get() : null;
        this.messages = messages;

        this.accumulatedText = new MessageTextAccumulator();
        this.currentMessage = 0;
        this.lastSection = RenderedMessage.getLastSection(getLastMessage());

        prefetchImages(this.messages);
    }

    public boolean append(RenderedMessage message) {
        return applyMessageModifier(message, appendMessage(message));
    }

    public boolean replace(RenderedMessage message) {
        return applyMessageModifier(message, replaceLastMessage(message));
    }

    private UnaryOperator<List<RenderedMessage>> appendMessage(RenderedMessage message) {
        return (List<RenderedMessage> currentMessages) -> {
            addAndUpdateLastSection(currentMessages, message);
            return currentMessages;
        };
    }

    private UnaryOperator<List<RenderedMessage>> replaceLastMessage(RenderedMessage message) {
        return (List<RenderedMessage> currentMessages) -> {
            removeLastMessageAndRebuildAccumulatedText(currentMessages);
            addAndUpdateLastSection(currentMessages, message);
            return currentMessages;
        };
    }

    private void removeLastMessageAndRebuildAccumulatedText(List<RenderedMessage> currentMessages) {
        currentMessages.remove(currentMessages.size() - 1);
        currentMessage--;
        accumulatedText = new MessageTextAccumulator();
        currentMessages.forEach(m -> m.forEach(accumulatedText::add));
    }

    private void addAndUpdateLastSection(List<RenderedMessage> currentMessages, RenderedMessage message) {
        currentMessages.add(message);
        lastSection = RenderedMessage.getLastSection(message);
    }

    private boolean applyMessageModifier(RenderedMessage message, UnaryOperator<List<RenderedMessage>> unaryOperator) {
        prefetchImages(message);
        try {
            messageRenderingInProgressLock.lockInterruptibly();
            try {
                // TODO Replace with design that matches requirements better:
                // + single RenderMessage instance per render queue
                // + single thread that never ends and blocking queue, or single threaded executor
                // + say, append, replace are equal ops (currently say is the main op, append/replace special add-ons)
                // +> MessageRenderer isn't a threaded media renderer anymore
                // +> RenderSay/Show/Append/Replace become MediaRenderer.THreaded
                // +> Replay becomes a threaded media renderer
                // TODO Resolve design flaws of the current approach:
                // - no queue, but the operations are sequential with a before/after relationship
                // - too many synchronizers
                // - too many states
                while (renderThread.get() != null && !hasCompletedMandatory() && messageModifier.get() != null) {
                    // messageRenderingModifierApplied.await(100, TimeUnit.MICROSECONDS);
                    messageRenderingModifierApplied.await();
                }
                messageModifier.set(unaryOperator);
                return !hasCompletedMandatory();
            } finally {
                messageRenderingInProgressLock.unlock();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ScriptInterruptedException(e);
        }
    }

    private RenderedMessage getLastMessage() {
        return this.messages.get(this.messages.size() - 1);
    }

    private void prefetchImages(List<RenderedMessage> messages) {
        for (RenderedMessage message : messages) {
            prefetchImages(message);
        }
    }

    private void prefetchImages(RenderedMessage message) {
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
        renderThread.set(Thread.currentThread());
        try {
            applyPendingMessageModifierAsResultOfReplay();

            do {
                boolean emptyMessage = messages.get(0).isEmpty();
                if (emptyMessage) {
                    show(null, Mood.Neutral);
                } else {
                    play();
                }

            } while (applyMessageModifer());
        } catch (InterruptedException | ScriptInterruptedException e) {
            cancelMessage();
            throw e;
        } finally {
            dead();
            signalMessageModifierApplied();
        }
    }

    private void cancelMessage() {
        if (currentRenderer != null) {
            renderQueue.interrupt(currentRenderer);
            currentRenderer = null;
        }
        if (backgroundSoundRenderer != null) {
            renderQueue.interrupt(backgroundSoundRenderer);
            backgroundSoundRenderer = null;
        }

        messageModifier.getAndSet(null);
    }

    private void dead() {
        renderThread.set(null);
    }

    private void applyPendingMessageModifierAsResultOfReplay() {
        if (!applyMessageModifer()) {
            if (messages.isEmpty()) {
                throw new IllegalStateException();
            }
        }
    }

    private boolean applyMessageModifer() {
        messageModifier.updateAndGet(this::updateMessageModifierState);
        return haveMoreMessages();
    }

    private UnaryOperator<List<RenderedMessage>> updateMessageModifierState(
            UnaryOperator<List<RenderedMessage>> operator) {
        if (operator != null) {
            operator.apply(messages);
        }

        if (!haveMoreMessages()) {
            finalizeRendering();
        }

        if (operator != null) {
            signalMessageModifierApplied();
        }
        return null;
    }

    private void signalMessageModifierApplied() {
        messageRenderingInProgressLock.lock();
        try {
            messageRenderingModifierApplied.signalAll();
        } finally {
            messageRenderingInProgressLock.unlock();
        }
    }

    private void play() throws IOException, InterruptedException {
        if (position == Position.FromStart) {
            accumulatedText = new MessageTextAccumulator();
            currentMessage = 0;
            renderMessages();
        } else if (position == Position.FromCurrentPosition) {
            Replayable replay;
            if (currentMessage < messages.size()) {
                replay = this::renderMessages;
            } else {
                replay = () -> renderMessage(getEnd());
            }
            replay.run();
        } else if (position == Position.FromMandatory) {
            // TODO remember accumulated text so that all but the last section
            // is displayed, rendered, but the text not added again
            // TODO Remove all but last speech and delay parts
            renderMessage(getMandatory());
        } else if (position == Position.End) {
            renderMessage(getEnd());
        } else {
            throw new IllegalStateException(position.toString());
        }
    }

    private RenderedMessage getMandatory() {
        return RenderedMessage.getLastSection(getLastMessage());
    }

    private RenderedMessage getEnd() {
        return stripAudio(RenderedMessage.getLastSection(getLastMessage()));
    }

    private static RenderedMessage stripAudio(AbstractMessage message) {
        return message.stream().filter(part -> !SoundTypes.contains(part.type)).collect(RenderedMessage.collector());
    }

    private void renderMessages() throws IOException, InterruptedException {
        while (haveMoreMessages()) {
            RenderedMessage message = messages.get(currentMessage);
            renderMessage(message);
            currentMessage++;

            boolean last = currentMessage == messages.size();
            if (!last && textToSpeechPlayer != null && !lastSectionHasDelay(message)) {
                renderTimeSpannedPart(delay(ScriptMessageDecorator.DELAY_BETWEEN_PARAGRAPHS_SECONDS));
            }
        }
    }

    private boolean haveMoreMessages() {
        return currentMessage < messages.size();
    }

    private static boolean lastSectionHasDelay(RenderedMessage message) {
        return message.getLastSection().contains(Type.Delay);
    }

    protected void finalizeRendering() {
        mandatoryCompleted();
        completeSectionAll();

        if (getTextToSpeech().isPresent() && !lastSection.contains(Type.Delay)) {
            renderTimeSpannedPart(delay(DELAY_AT_END_OF_MESSAGE));
            completeSectionMandatory();
            completeSectionAll();
        }
    }

    /**
     * 
     * @param message
     * @throws IOException
     * @throws InterruptedException
     */
    private void renderMessage(RenderedMessage message) throws IOException, InterruptedException {
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
                renderPart(part, accumulatedText, mood);
            }

            completeSectionAll();
            if (part.type == Message.Type.Text) {
                show(part.value, accumulatedText, mood);
            } else if (lastPart && definesPageLayout(part)) {
                show(accumulatedText.toString());
            }

            if (Thread.currentThread().isInterrupted()) {
                break;
            }
        }
    }

    private static boolean definesPageLayout(MessagePart part) {
        return part.type == Type.Image || part.type == Type.Text;
    }

    private void renderPart(MessagePart part, MessageTextAccumulator accumulatedText, String mood)
            throws IOException, InterruptedException {
        if (part.type == Message.Type.Image) {
            displayImage = part.value;
        } else if (part.type == Message.Type.BackgroundSound) {
            playSoundAsynchronous(part);
            // use awaitSoundCompletion keyword to wait for background sound completion
        } else if (part.type == Message.Type.Sound) {
            playSound(part);
        } else if (part.type == Message.Type.Speech) {
            playSpeech(part, mood);
        } else if (part.type == Message.Type.DesktopItem) {
            if (isInstructionalImageOutputEnabled()) {
                try {
                    showDesktopItem(part);
                } catch (IOException e) {
                    showDesktopItemError(accumulatedText, mood, e);
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

    private void renderTimeSpannedPart(MediaRenderer.Threaded renderer) {
        if (this.currentRenderer != null) {
            this.currentRenderer.completeMandatory();
        }
        this.currentRenderer = renderer;
        renderQueue.submit(this.currentRenderer);
    }

    private void showDesktopItem(MessagePart part) throws IOException {
        RenderDesktopItem renderDesktopItem = new RenderDesktopItem(teaseLib, resources, part.value);
        completeSectionAll();
        renderQueue.submit(renderDesktopItem);
    }

    private void showDesktopItemError(MessageTextAccumulator accumulatedText, String mood, IOException e)
            throws IOException, InterruptedException {
        accumulatedText.add(new MessagePart(Message.Type.Text, e.getMessage()));
        completeSectionAll();
        show(accumulatedText.toString(), mood);
    }

    private void playSpeech(MessagePart part, String mood) throws IOException {
        if (Message.Type.isSound(part.value)) {
            renderTimeSpannedPart(new RenderPrerecordedSpeech(part.value, resources, teaseLib));
        } else if (TextToSpeechPlayer.isSimulatedSpeech(part.value)) {
            renderTimeSpannedPart(
                    new RenderSpeechDelay(TextToSpeechPlayer.getSimulatedSpeechText(part.value), teaseLib));
        } else if (isSpeechOutputEnabled() && textToSpeechPlayer != null) {
            renderTimeSpannedPart(new RenderTTSSpeech(textToSpeechPlayer, actor, part.value, mood, teaseLib));
        } else {
            renderTimeSpannedPart(new RenderSpeechDelay(part.value, teaseLib));
        }
    }

    private void playSound(MessagePart part) throws IOException {
        if (isSoundOutputEnabled()) {
            renderTimeSpannedPart(new RenderSound(resources, part.value, teaseLib));
        }
    }

    private void playSoundAsynchronous(MessagePart part) throws IOException {
        if (isSoundOutputEnabled()) {
            completeSectionMandatory();
            if (backgroundSoundRenderer != null) {
                renderQueue.interrupt(backgroundSoundRenderer);
            }
            backgroundSoundRenderer = new RenderSound(resources, part.value, teaseLib);
            renderQueue.submit(backgroundSoundRenderer);
        }
    }

    private void show(String text, MessageTextAccumulator accumulatedText, String mood)
            throws IOException, InterruptedException {
        teaseLib.transcript.info(text);
        show(accumulatedText.toString(), mood);
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

    private void show(String text, String mood) throws IOException, InterruptedException {
        logMoodToTranscript(mood);
        logImageToTranscript();
        show(text);
    }

    private void logMoodToTranscript(String mood) {
        if (actor.images.contains(displayImage) && mood != Mood.Neutral) {
            teaseLib.transcript.info("mood = " + mood);
        }
    }

    private void logImageToTranscript() {
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
        if (!Thread.currentThread().isInterrupted()) {
            teaseLib.host.show(getImageBytes(), text);
            // First message shown - start completed
            startCompleted();
        }
    }

    private byte[] getImageBytes() throws IOException, InterruptedException {
        if (displayImage != null && displayImage != Message.NoImage) {
            try {
                return imageFetcher.get(displayImage);
            } catch (IOException e) {
                handleIOException(e);
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
            if (backgroundSoundRenderer != null) {
                backgroundSoundRenderer.completeAll();
                backgroundSoundRenderer = null;
            }
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
            double delay = geteDelaySeconds(part.value);
            if (delay > 0) {
                renderTimeSpannedPart(delay(delay));
            }
        }
    }

    private RenderDelay delay(double seconds) {
        return new RenderDelay(seconds, seconds > ScriptMessageDecorator.DELAY_BETWEEN_PARAGRAPHS_SECONDS, teaseLib);
    }

    private double geteDelaySeconds(String args) {
        double[] argv = getDelayInterval(args);
        if (argv.length == 1) {
            return argv[0];
        } else {
            return teaseLib.random(argv[0], argv[1]);
        }
    }

    // TODO Utilities -> Interval
    public static double[] getDelayInterval(String delayInterval) {
        String[] argv = delayInterval.split(" ");
        if (argv.length == 1) {
            return new double[] { Double.parseDouble(delayInterval) };
        } else {
            return new double[] { Double.parseDouble(argv[0]), Double.parseDouble(argv[1]) };
        }
    }

    private static Interval getDelayMillis(String args) {
        double[] argv = getDelayInterval(args);
        if (argv.length == 1) {
            int delay = (int) (argv[0] * 1000.0);
            return new Interval(delay, delay);
        } else {
            return new Interval((int) (argv[0] * 1000.0), (int) (argv[1] * 1000.0));
        }
    }

    @Override
    public String toString() {
        long delayMillis = 0;
        MessageTextAccumulator text = new MessageTextAccumulator();
        for (RenderedMessage message : messages) {
            AbstractMessage paragraphs = message;
            for (Iterator<MessagePart> it = paragraphs.iterator(); it.hasNext();) {
                MessagePart part = it.next();
                text.add(part);
                if (part.type == Type.Text) {
                    delayMillis += TextToSpeech.getEstimatedSpeechDuration(part.value);
                } else if (part.type == Type.Delay) {
                    delayMillis += getDelayMillis(part.value).start;
                }
            }
        }
        String messageText = text.toString().replace("\n", " ");
        int length = 40;
        return "delay~" + String.format("%.2f", (double) delayMillis / 1000) + " Message='"
                + (messageText.length() > length ? messageText.substring(0, length) + "..." : messageText + "'");
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
        return Optional.ofNullable(textToSpeechPlayer);
    }
}
