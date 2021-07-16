package teaselib.core.media;

import static teaselib.core.concurrency.NamedExecutorService.singleThreadedQueue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.BinaryOperator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import teaselib.Actor;
import teaselib.Config;
import teaselib.Images;
import teaselib.Message;
import teaselib.Message.Type;
import teaselib.MessagePart;
import teaselib.Mood;
import teaselib.Replay;
import teaselib.Replay.Position;
import teaselib.Replay.Replayable;
import teaselib.core.AbstractImages;
import teaselib.core.Closeable;
import teaselib.core.ResourceLoader;
import teaselib.core.ScriptEventArgs.BeforeNewMessage.OutlineType;
import teaselib.core.ScriptInterruptedException;
import teaselib.core.TeaseLib;
import teaselib.core.concurrency.NamedExecutorService;
import teaselib.core.texttospeech.TextToSpeechPlayer;
import teaselib.core.util.ExceptionUtil;
import teaselib.util.AnnotatedImage;

public class SectionRenderer implements Closeable {

    static final Logger logger = LoggerFactory.getLogger(SectionRenderer.class);

    private static final Set<Message.Type> ManuallyLoggedMessageTypes = new HashSet<>(Arrays.asList(Message.Type.Text,
            Message.Type.Image, Message.Type.Mood, Message.Type.Sound, Message.Type.Speech, Message.Type.Delay));
    static final Set<Type> SoundTypes = new HashSet<>(Arrays.asList(Type.Speech, Type.Sound, Type.BackgroundSound));

    private final TeaseLib teaseLib;
    private final MediaRendererQueue renderQueue;
    // TODO Handle message decorator processing here in order to make textToSpeechPlayer private
    public final TextToSpeechPlayer textToSpeechPlayer;

    final NamedExecutorService executor = singleThreadedQueue("Message renderer queue", 1, TimeUnit.HOURS);
    Future<?> running = null;

    // TODO workaround to tell section renderer to add the proper delay for the next message
    public OutlineType nextOutlineType;
    private MediaRenderer.Threaded currentRenderer = null;
    private RenderSound backgroundSoundRenderer = null;

    public SectionRenderer(TeaseLib teaseLib, MediaRendererQueue renderQueue) {
        this.teaseLib = teaseLib;
        this.renderQueue = renderQueue;
        this.textToSpeechPlayer = new TextToSpeechPlayer(teaseLib.config);
    }

    @Override
    public void close() {
        executor.shutdown();
        executor.getQueue().drainTo(new ArrayList<>());
        if (textToSpeechPlayer != null) {
            textToSpeechPlayer.close();
        }
    }

    public MediaRenderer.Threaded say(Actor actor, List<RenderedMessage> messages, ResourceLoader resources) {
        return createBatch(actor, messages, say, resources);
    }

    public MediaRenderer.Threaded append(Actor actor, List<RenderedMessage> messages, ResourceLoader resources) {
        return createBatch(actor, messages, append, resources);
    }

    public MediaRenderer.Threaded replace(Actor actor, List<RenderedMessage> messages, ResourceLoader resources) {
        return createBatch(actor, messages, replace, resources);
    }

    public MediaRenderer.Threaded showAll(Actor actor, List<RenderedMessage> messages, ResourceLoader resources) {
        return createBatch(actor, messages, showAll, resources);
    }

    public void showAll(double delaySeconds) {
        if (delaySeconds > ScriptMessageDecorator.DELAY_BETWEEN_PARAGRAPHS_SECONDS) {
            executor.submit(() -> {
                try {
                    var actual = currentMessageRenderer;
                    if (actual != null) {
                        completeSectionMandatory();
                        renderTimeSpannedPart(
                                delay(delaySeconds - ScriptMessageDecorator.DELAY_BETWEEN_PARAGRAPHS_SECONDS));
                        currentRenderer.completeMandatory();
                        show(actual);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new ScriptInterruptedException(e);
                } catch (IOException e) {
                    ExceptionUtil.handleIOException(e, teaseLib.config, logger);
                }
                return null;
            });
        } else {
            var current = currentMessageRenderer;
            if (current != null) {
                current.renderer.getTask().cancel(true);
                // TODO show appended messages as single paragraphs again, then show all appended
                try {
                    show(current);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new ScriptInterruptedException(e);
                } catch (IOException e) {
                    try {
                        ExceptionUtil.handleIOException(e, teaseLib.config, logger);
                    } catch (IOException e1) {
                        throw ExceptionUtil.asRuntimeException(e1);
                    }
                }
            }
        }

    }

    private final class Batch extends MessageRenderer {
        private Batch(Actor actor, List<RenderedMessage> messages, BinaryOperator<MessageRenderer> operator,
                ResourceLoader resources) {
            super(actor, messages, operator, resources);
        }

        @Override
        public void run() {
            currentMessageRenderer = applyOperator();
            completePreviousTask();
            submitTask();
        }

        private MessageRenderer applyOperator() {
            return currentMessageRenderer == null ? this : this.operator.apply(currentMessageRenderer, this);
        }

        private void completePreviousTask() {
            if (running != null && !running.isCancelled() && !running.isDone()) {
                try {
                    running.get();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new ScriptInterruptedException(e);
                } catch (ExecutionException e) {
                    // TODO Proper handling of IO exceptions
                    running = null;
                    throw ExceptionUtil.asRuntimeException(ExceptionUtil.reduce(e));
                }
            }
        }

        private void submitTask() {
            Future<Void> future = executor.submit(() -> {
                try {
                    SectionRenderer.this.run(currentMessageRenderer);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new ScriptInterruptedException(e);
                } catch (IOException e) {
                    ExceptionUtil.handleIOException(e, teaseLib.config, logger);
                }
                return null;
            });

            running = thisTask = new MediaFutureTask<>(renderer, future) {
                @Override
                public boolean cancel(boolean mayInterruptIfRunning) {
                    boolean cancel = super.cancel(mayInterruptIfRunning);
                    renderer.completedStart.countDown();
                    renderer.completedMandatory.countDown();
                    renderer.completedAll.countDown();
                    return cancel;
                }
            };
        }
    }

    private MessageRenderer currentMessageRenderer = null;

    public MediaRenderer.Threaded createBatch(Actor actor, List<RenderedMessage> messages,
            BinaryOperator<MessageRenderer> operator, ResourceLoader resources) {
        MessageRenderer next = new Batch(actor, messages, operator, resources);
        prefetchImages(next);
        return next.renderer;
    }

    private void prefetchImages(MessageRenderer messageRenderer) {
        prefetchImages(messageRenderer.actor, messageRenderer.messages);
    }

    private void prefetchImages(Actor actor, List<RenderedMessage> messages) {
        if (actor.images != Images.None) {
            for (RenderedMessage message : messages) {
                prefetchImages(actor, message);
            }
            if (actor.images instanceof AbstractImages) {
                ((AbstractImages) actor.images).prefetcher().fetch();
            }
        }
    }

    private void prefetchImages(Actor actor, RenderedMessage message) {
        for (MessagePart part : message) {
            if (part.type == Message.Type.Image) {
                String resource = part.value;
                if (!Message.NoImage.equals(part.value) && actor.images instanceof AbstractImages) {
                    ((AbstractImages) actor.images).prefetcher().add(resource);
                }
            }
        }
    }

    static BinaryOperator<MessageRenderer> say = (current, next) -> next;

    static BinaryOperator<MessageRenderer> append = (current, next) -> {
        List<RenderedMessage> messages = current.messages;
        copy(current, next, messages);
        next.currentMessage = current.currentMessage;
        return next;
    };

    static BinaryOperator<MessageRenderer> replace = (current, next) -> {
        List<RenderedMessage> messages = new ArrayList<>(current.messages);
        messages.remove(messages.size() - 1);
        copy(current, next, messages);
        next.currentMessage = current.currentMessage - 1;
        return next;
    };

    static BinaryOperator<MessageRenderer> showAll = (current, next) -> {
        List<RenderedMessage> messages = new ArrayList<>(current.messages);
        copy(current, next, messages);
        next.position = Replay.Position.End;
        next.currentMessage = next.messages.size() - 1;
        return next;
    };

    private static void copy(MessageRenderer batch, MessageRenderer next, List<RenderedMessage> messages) {
        next.accumulatedText = new MessageTextAccumulator();
        messages.forEach(m -> m.forEach(next.accumulatedText::add));
        next.messages.addAll(0, messages);
    }

    public void run(MessageRenderer messageRenderer) throws InterruptedException, IOException {
        try {
            // TOOO Avoid locks caused by interrupting before start
            messageRenderer.renderer.startCompleted();

            boolean emptyMessage = messageRenderer.messages.get(0).isEmpty();
            if (emptyMessage) {
                show(messageRenderer, Mood.Neutral);
                messageRenderer.renderer.startCompleted();
            } else {
                play(messageRenderer);
            }

            messageRenderer.renderer.mandatoryCompleted();
            finalizeRendering(messageRenderer);
        } catch (InterruptedException | ScriptInterruptedException e) {
            if (currentRenderer != null) {
                renderQueue.interrupt(currentRenderer);
                currentRenderer = null;
            }
            if (backgroundSoundRenderer != null) {
                renderQueue.interrupt(backgroundSoundRenderer);
                backgroundSoundRenderer = null;
            }
            throw e;
        } finally {
            messageRenderer.renderer.startCompleted();
            messageRenderer.renderer.mandatoryCompleted();
            messageRenderer.renderer.allCompleted();
        }
    }

    private void play(MessageRenderer messageRenderer) throws IOException, InterruptedException {
        // TODO Move to batch and return the runnable to render

        if (messageRenderer.position == Position.FromStart) {
            messageRenderer.accumulatedText = new MessageTextAccumulator();
            messageRenderer.currentMessage = 0;
            renderMessages(messageRenderer);
        } else if (messageRenderer.position == Position.FromCurrentPosition) {
            Replayable replay;
            if (messageRenderer.currentMessage < messageRenderer.messages.size()) {
                replay = () -> renderMessages(messageRenderer);
            } else {
                replay = () -> renderMessage(messageRenderer, messageRenderer.getEnd());
            }
            replay.run();
        } else if (messageRenderer.position == Position.FromMandatory) {
            // TODO remember accumulated text so that all but the last section
            // is displayed, rendered, but the text not added again
            // TODO Remove all but last speech and delay parts
            renderMessage(messageRenderer, messageRenderer.getMandatory());
        } else if (messageRenderer.position == Position.End) {
            completeSectionMandatory();
            renderMessages(messageRenderer);
            show(messageRenderer, messageRenderer.accumulatedText.paragraphs);

        } else {
            throw new IllegalStateException(messageRenderer.position.toString());
        }
    }

    private void renderMessages(MessageRenderer messageRenderer) throws IOException, InterruptedException {
        while (haveMoreMessages(messageRenderer)) {
            RenderedMessage message = messageRenderer.messages.get(messageRenderer.currentMessage);
            try {
                renderMessage(messageRenderer, message);
            } finally {
                messageRenderer.currentMessage++;
            }
            renderOptionalDefaultDelayBetweenMultipleMessages(messageRenderer);
        }
    }

    private void renderOptionalDefaultDelayBetweenMultipleMessages(MessageRenderer messageRenderer) {
        if (textToSpeechPlayer != null) {
            boolean last = messageRenderer.currentMessage == messageRenderer.messages.size()
                    && nextOutlineType != OutlineType.AppendParagraph;
            if (!last && !messageRenderer.lastSection.contains(Type.Delay)) {
                renderTimeSpannedPart(delay(ScriptMessageDecorator.DELAY_BETWEEN_PARAGRAPHS_SECONDS));
            }
        }
    }

    private boolean haveMoreMessages(MessageRenderer messageRenderer) {
        return messageRenderer.currentMessage < messageRenderer.messages.size();
    }

    protected void finalizeRendering(MessageRenderer messageRenderer) {
        completeSectionMandatory();
        if (nextOutlineType == OutlineType.NewSection && textToSpeechPlayer != null
                && !messageRenderer.lastSection.contains(Type.Delay)) {
            renderSectionEndDelay();
        }
        completeSectionAll();
    }

    private void renderSectionEndDelay() {
        completeSectionMandatory();
        renderTimeSpannedPart(delay(ScriptMessageDecorator.DELAY_BETWEEN_SECTIONS_SECONDS));
    }

    private void renderMessage(MessageRenderer messageRenderer, RenderedMessage message)
            throws IOException, InterruptedException {
        String mood = Mood.Neutral;
        for (Iterator<MessagePart> it = message.iterator(); it.hasNext();) {
            MessagePart part = it.next();
            if (!ManuallyLoggedMessageTypes.contains(part.type)) {
                teaseLib.transcript.info("" + part.type.name() + " = " + part.value);
            }
            logger.info("{}={}", part.type, part.value);

            if (part.type == Message.Type.Mood) {
                mood = part.value;
            } else {
                renderPart(part, messageRenderer, mood);
            }
            completeSectionAll();

            if (part.type == Message.Type.Text || (!it.hasNext() && part.type == Message.Type.Image)) {
                show(messageRenderer, mood);
                messageRenderer.renderer.startCompleted();
            }

            if (Thread.currentThread().isInterrupted()) {
                break;
            }
        }
    }

    private void renderPart(MessagePart part, MessageRenderer messageRenderer, String mood)
            throws IOException, InterruptedException {
        if (part.type == Message.Type.Image) {
            messageRenderer.displayImage = part.value;
        } else if (part.type == Message.Type.BackgroundSound) {
            playSoundAsynchronous(part, messageRenderer.resources);
            // use awaitSoundCompletion keyword to wait for background sound completion
        } else if (part.type == Message.Type.Sound) {
            playSound(part, messageRenderer.resources);
        } else if (part.type == Message.Type.Speech) {
            playSpeech(messageRenderer.actor, part, mood, messageRenderer.resources);
        } else if (part.type == Message.Type.DesktopItem) {
            if (isInstructionalImageOutputEnabled()) {
                try {
                    showDesktopItem(part, messageRenderer.resources);
                } catch (IOException e) {
                    showDesktopItemError(messageRenderer, messageRenderer.accumulatedText, mood, e);
                    throw e;
                }
            }
        } else if (part.type == Message.Type.Keyword) {
            doKeyword(messageRenderer, part);
        } else if (part.type == Message.Type.Delay) {
            doDelay(part);
        } else if (part.type == Message.Type.Item) {
            messageRenderer.accumulatedText.add(part);
        } else if (part.type == Message.Type.Text) {
            messageRenderer.accumulatedText.add(part);
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

    private void showDesktopItem(MessagePart part, ResourceLoader resources) throws IOException {
        var renderDesktopItem = new RenderDesktopItem(teaseLib, resources, part.value);
        completeSectionAll();
        renderQueue.submit(renderDesktopItem);
    }

    private void showDesktopItemError(MessageRenderer messageRenderer, MessageTextAccumulator accumulatedText,
            String mood, IOException e) throws IOException, InterruptedException {
        accumulatedText.add(new MessagePart(Message.Type.Text, e.getMessage()));
        completeSectionAll();
        show(messageRenderer, mood);
        messageRenderer.renderer.startCompleted();
    }

    private void playSpeech(Actor actor, MessagePart part, String mood, ResourceLoader resources) throws IOException {
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

    private void playSound(MessagePart part, ResourceLoader resources) throws IOException {
        if (isSoundOutputEnabled()) {
            renderTimeSpannedPart(new RenderSound(resources, part.value, teaseLib));
        }
    }

    private void playSoundAsynchronous(MessagePart part, ResourceLoader resources) throws IOException {
        if (isSoundOutputEnabled()) {
            completeSectionMandatory();
            if (backgroundSoundRenderer != null) {
                renderQueue.interrupt(backgroundSoundRenderer);
            }
            backgroundSoundRenderer = new RenderSound(resources, part.value, teaseLib);
            renderQueue.submit(backgroundSoundRenderer);
        }
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

    private void show(MessageRenderer message, String mood) throws IOException, InterruptedException {
        var transcript = new StringBuilder();
        if (message.hasActorImage()) {
            teaseLib.transcript.debug("image = '" + message.displayImage + "'");
            if (!Mood.Neutral.equalsIgnoreCase(mood)) {
                transcript.append(mood);
                transcript.append(" ");
            }
        } else if (!Message.NoImage.equalsIgnoreCase(message.displayImage)) {
            teaseLib.transcript.info("image = '" + message.displayImage + "'");
        }

        var text = message.accumulatedText.getTail();
        if (text != null && !text.isBlank()) {
            transcript.append(text);
        }
        teaseLib.transcript.info(transcript.toString());

        show(message, Collections.singletonList(text));
    }

    private void show(MessageRenderer message) throws IOException, InterruptedException {
        show(message, message.accumulatedText.paragraphs);
    }

    private void show(MessageRenderer message, List<String> paragraphs) throws IOException, InterruptedException {
        if (!Thread.currentThread().isInterrupted()) {
            teaseLib.host.show(annotatedImage(message.actor, message.displayImage), paragraphs);
            teaseLib.host.show();
        }
    }

    private AnnotatedImage annotatedImage(Actor actor, String displayImage) throws IOException, InterruptedException {
        if (displayImage != null && !Message.NoImage.equals(displayImage)) {
            try {
                return actor.images.annotated(displayImage);
            } catch (IOException e) {
                handleIOException(e);
                return AnnotatedImage.NoImage;
            } finally {
                if (actor.images instanceof AbstractImages) {
                    ((AbstractImages) actor.images).prefetcher().fetch();
                }
            }
        } else {
            return AnnotatedImage.NoImage;
        }
    }

    protected void handleIOException(IOException e) throws IOException {
        ExceptionUtil.handleIOException(e, teaseLib.config, logger);
    }

    private void doKeyword(MessageRenderer messageRenderer, MessagePart part) {
        String keyword = part.value;
        if (Message.ActorImage.equalsIgnoreCase(keyword)) {
            throw new IllegalStateException(keyword + " must be resolved in pre-parse");
        } else if (Message.NoImage.equalsIgnoreCase(keyword)) {
            throw new IllegalStateException(keyword + " must be resolved in pre-parse");
        } else if (Message.ShowChoices.equalsIgnoreCase(keyword)) {
            completeSectionMandatory();
            messageRenderer.renderer.mandatoryCompleted();
        } else if (Message.AwaitSoundCompletion.equalsIgnoreCase(keyword)) {
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
        boolean logToTranscript = seconds > ScriptMessageDecorator.DELAY_BETWEEN_SECTIONS_SECONDS;
        return new RenderDelay(seconds, logToTranscript, teaseLib);
    }

    private double geteDelaySeconds(String args) {
        double[] argv = getDelayInterval(args);
        if (argv.length == 1) {
            return argv[0];
        } else {
            return teaseLib.random.value(argv[0], argv[1]);
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
