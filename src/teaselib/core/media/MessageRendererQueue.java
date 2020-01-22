package teaselib.core.media;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.BinaryOperator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import teaselib.Actor;
import teaselib.Config;
import teaselib.Message;
import teaselib.Message.Type;
import teaselib.MessagePart;
import teaselib.Mood;
import teaselib.Replay;
import teaselib.Replay.Position;
import teaselib.Replay.Replayable;
import teaselib.core.AbstractMessage;
import teaselib.core.Closeable;
import teaselib.core.ResourceLoader;
import teaselib.core.ScriptInterruptedException;
import teaselib.core.TeaseLib;
import teaselib.core.concurrency.NamedExecutorService;
import teaselib.core.texttospeech.TextToSpeechPlayer;
import teaselib.core.util.ExceptionUtil;
import teaselib.core.util.PrefetchImage;
import teaselib.core.util.Prefetcher;

public class MessageRendererQueue implements Closeable {
    static final Logger logger = LoggerFactory.getLogger(MessageRendererQueue.class);

    private static final Set<Message.Type> ManuallyLoggedMessageTypes = new HashSet<>(Arrays.asList(Message.Type.Text,
            Message.Type.Image, Message.Type.Mood, Message.Type.Speech, Message.Type.Delay));
    static final Set<Type> SoundTypes = new HashSet<>(Arrays.asList(Type.Speech, Type.Sound, Type.BackgroundSound));

    private static final double DELAY_AT_END_OF_MESSAGE = 2.0;

    private final TeaseLib teaseLib;
    private final MediaRendererQueue renderQueue;
    // TODO Handle message decorator processing here in order to make textToSpeechPlayer private
    public final TextToSpeechPlayer textToSpeechPlayer;

    final NamedExecutorService executor = NamedExecutorService.singleThreadedQueue("Message renderer queue", 1,
            TimeUnit.HOURS);
    private final Prefetcher<byte[]> imageFetcher;
    Future<?> running = null;

    private MediaRenderer.Threaded currentRenderer = null;
    private RenderSound backgroundSoundRenderer = null;

    public MessageRendererQueue(TeaseLib teaseLib, MediaRendererQueue renderQueue) {
        this.teaseLib = teaseLib;
        this.renderQueue = renderQueue;
        this.imageFetcher = new Prefetcher<>(renderQueue.getExecutorService());
        this.textToSpeechPlayer = new TextToSpeechPlayer(teaseLib.config);
    }

    @Override
    public void close() {
        executor.shutdown();
        executor.getQueue().drainTo(new ArrayList<>());
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

    Batch current = null;

    public MediaRenderer.Threaded createBatch(Actor actor, List<RenderedMessage> messages,
            BinaryOperator<Batch> operator, ResourceLoader resources) {
        Batch next = new Batch(actor, messages, operator, resources) {
            @Override
            public void run() {
                current = applyOperator();
                completePreviousTask();
                submitTask();
            }

            private Batch applyOperator() {
                return current == null ? this : this.operator.apply(current, this);
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
                Future<?> future = executor.submit(() -> {
                    try {
                        MessageRendererQueue.this.run(current);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new ScriptInterruptedException(e);
                    } catch (IOException e) {
                        // TODO Proper handling of IO exceptions
                        throw ExceptionUtil.asRuntimeException(e);
                    }
                });

                running = thisTask = new MediaFutureTask<RendererFacade>(renderer, (Future<Void>) future) {
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
        };

        prefetchImages(next);
        return next.renderer;
    }

    private void prefetchImages(Batch batch) {
        prefetchImages(batch.messages, batch.resources);
    }

    private void prefetchImages(List<RenderedMessage> messages, ResourceLoader resources) {
        for (RenderedMessage message : messages) {
            prefetchImages(message, resources);
        }
    }

    private void prefetchImages(RenderedMessage message, ResourceLoader resources) {
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

    static BinaryOperator<Batch> say = (batch, next) -> {
        return next;
    };

    static BinaryOperator<Batch> append = (batch, next) -> {
        List<RenderedMessage> current = batch.messages;

        next.accumulatedText = new MessageTextAccumulator();
        current.forEach(m -> m.forEach(next.accumulatedText::add));
        next.messages.addAll(0, current);

        next.position = batch.position;
        next.currentMessage = batch.currentMessage;

        return next;
    };

    static BinaryOperator<Batch> replace = (batch, next) -> {
        // return batch.removeLastMessageAndRebuildAccumulatedText().addAndUpdateLastSection(next.messages);

        List<RenderedMessage> current = new ArrayList<>(batch.messages);
        current.remove(current.size() - 1);

        next.accumulatedText = new MessageTextAccumulator();
        current.forEach(m -> m.forEach(next.accumulatedText::add));
        next.messages.addAll(0, current);

        next.position = batch.position;
        next.currentMessage = batch.currentMessage - 1;

        return next;
    };

    public abstract static class Batch implements Runnable {
        final Actor actor;
        final List<RenderedMessage> messages;
        final ResourceLoader resources;
        final BinaryOperator<Batch> operator;

        Future<Void> thisTask = null;
        RendererFacade renderer = new RendererFacade();
        Replay.Position position = Replay.Position.FromCurrentPosition;
        MessageTextAccumulator accumulatedText = new MessageTextAccumulator();

        int currentMessage = 0;
        String displayImage = null;
        AbstractMessage lastSection;

        public Batch(Actor actor, List<RenderedMessage> messages, BinaryOperator<Batch> operator,
                ResourceLoader resources) {
            this.actor = actor;
            this.resources = resources;
            this.messages = messages;
            this.operator = operator;

            this.lastSection = RenderedMessage.getLastSection(getLastMessage());
        }

        private RenderedMessage getLastMessage() {
            return getLastMessage(this.messages);
        }

        private static RenderedMessage getLastMessage(List<RenderedMessage> messages) {
            return messages.get(messages.size() - 1);
        }

        RenderedMessage getMandatory() {
            return RenderedMessage.getLastSection(getLastMessage());
        }

        RenderedMessage getEnd() {
            return stripAudio(RenderedMessage.getLastSection(getLastMessage()));
        }

        private static RenderedMessage stripAudio(AbstractMessage message) {
            return message.stream().filter(part -> !SoundTypes.contains(part.type))
                    .collect(RenderedMessage.collector());
        }

        // TODO Contains duplicated code from ThreadedMediaRenderer
        class RendererFacade implements MediaRenderer.Threaded {
            final CountDownLatch completedStart = new CountDownLatch(1);
            final CountDownLatch completedMandatory = new CountDownLatch(1);;
            final CountDownLatch completedAll = new CountDownLatch(1);;

            private long startMillis = 0;

            @Override
            public void run() {
                startMillis = System.currentTimeMillis();
                Batch.this.run();
            }

            @Override
            public void completeStart() {
                try {
                    // TODO Blocks in PCM tests because the message renderer task is cancelled,
                    // before startCompleted is reached - executor is idle
                    completedStart.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new ScriptInterruptedException(e);
                }
            }

            @Override
            public void completeMandatory() {
                try {
                    completedMandatory.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new ScriptInterruptedException(e);
                }
            }

            @Override
            public void completeAll() {
                try {
                    completedAll.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new ScriptInterruptedException(e);
                }
            }

            protected final void startCompleted() {
                completedStart.countDown();
                if (logger.isDebugEnabled()) {
                    logger.debug(getClass().getSimpleName() + " completed start after "
                            + String.format("%.2f seconds", getElapsedSeconds()));
                }
            }

            protected final void mandatoryCompleted() {
                completedMandatory.countDown();
                if (logger.isDebugEnabled()) {
                    logger.debug("{} completed mandatory after {}", getClass().getSimpleName(),
                            String.format("%.2f seconds", getElapsedSeconds()));
                }
            }

            protected final void allCompleted() {
                completedAll.countDown();
                if (logger.isDebugEnabled()) {
                    logger.debug("{} completed all after {}", getClass().getSimpleName(), getElapsedSecondsFormatted());
                }
            }

            @Override
            public boolean hasCompletedStart() {
                return completedStart.getCount() == 0;
            }

            @Override
            public boolean hasCompletedMandatory() {
                return completedMandatory.getCount() == 0;
            }

            @Override
            public boolean hasCompletedAll() {
                return completedAll.getCount() == 0;
            }

            public String getElapsedSecondsFormatted() {
                return String.format("%.2f", getElapsedSeconds());
            }

            private double getElapsedSeconds() {
                return (System.currentTimeMillis() - startMillis) / 1000.0;
            }

            public Future<Void> getTask() {
                return Batch.this.thisTask;
            }
        }

    }

    public void run(Batch batch) throws InterruptedException, IOException {
        try {
            // TOOO Avoid locks caused by interrupting before start
            batch.renderer.startCompleted();

            boolean emptyMessage = batch.messages.get(0).isEmpty();
            if (emptyMessage) {
                show(batch.actor, null, Mood.Neutral, batch.displayImage);
                batch.renderer.startCompleted();
            } else {
                play(batch);
            }
            finalizeRendering(batch);
            batch.renderer.allCompleted();
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
            batch.renderer.startCompleted();
            batch.renderer.mandatoryCompleted();
            batch.renderer.allCompleted();
        }
    }

    private void play(Batch batch) throws IOException, InterruptedException {
        // TODO Move to batch and return the runnable to render

        if (batch.position == Position.FromStart) {
            batch.accumulatedText = new MessageTextAccumulator();
            batch.currentMessage = 0;
            renderMessages(batch);
        } else if (batch.position == Position.FromCurrentPosition) {
            Replayable replay;
            if (batch.currentMessage < batch.messages.size()) {
                replay = () -> renderMessages(batch);
            } else {
                replay = () -> renderMessage(batch, batch.getEnd());
            }
            replay.run();
        } else if (batch.position == Position.FromMandatory) {
            // TODO remember accumulated text so that all but the last section
            // is displayed, rendered, but the text not added again
            // TODO Remove all but last speech and delay parts
            renderMessage(batch, batch.getMandatory());
        } else if (batch.position == Position.End) {
            renderMessage(batch, batch.getEnd());
        } else {
            throw new IllegalStateException(batch.position.toString());
        }
    }

    private void renderMessages(Batch batch) throws IOException, InterruptedException {
        while (haveMoreMessages(batch)) {
            RenderedMessage message = batch.messages.get(batch.currentMessage);
            renderMessage(batch, message);
            batch.currentMessage++;

            boolean last = batch.currentMessage == batch.messages.size();
            if (!last && textToSpeechPlayer != null && !lastSectionHasDelay(message)) {
                renderTimeSpannedPart(delay(ScriptMessageDecorator.DELAY_BETWEEN_PARAGRAPHS_SECONDS));
            }
        }
    }

    private boolean haveMoreMessages(Batch batch) {
        return batch.currentMessage < batch.messages.size();
    }

    private static boolean lastSectionHasDelay(RenderedMessage message) {
        return message.getLastSection().contains(Type.Delay);
    }

    protected void finalizeRendering(Batch batch) {
        batch.renderer.mandatoryCompleted();
        completeSectionAll();

        if (executor.getQueue().isEmpty() && getTextToSpeech().isPresent() && !batch.lastSection.contains(Type.Delay)) {
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
    private void renderMessage(Batch batch, RenderedMessage message) throws IOException, InterruptedException {
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
                renderPart(part, batch, mood);
            }

            completeSectionAll();
            if (part.type == Message.Type.Text) {
                show(batch.actor, part.value, batch.accumulatedText, mood, batch.displayImage);
                batch.renderer.startCompleted();
            } else if (lastPart && definesPageLayout(part)) {
                show(batch.accumulatedText.toString(), batch.displayImage);
                batch.renderer.startCompleted();
            }

            if (Thread.currentThread().isInterrupted()) {
                break;
            }
        }
    }

    private static boolean definesPageLayout(MessagePart part) {
        return part.type == Type.Image || part.type == Type.Text;
    }

    private void renderPart(MessagePart part, Batch batch, String mood) throws IOException, InterruptedException {
        if (part.type == Message.Type.Image) {
            batch.displayImage = part.value;
        } else if (part.type == Message.Type.BackgroundSound) {
            playSoundAsynchronous(part, batch.resources);
            // use awaitSoundCompletion keyword to wait for background sound completion
        } else if (part.type == Message.Type.Sound) {
            playSound(part, batch.resources);
        } else if (part.type == Message.Type.Speech) {
            playSpeech(batch.actor, part, mood, batch.resources);
        } else if (part.type == Message.Type.DesktopItem) {
            if (isInstructionalImageOutputEnabled()) {
                try {
                    showDesktopItem(part, batch.resources);
                } catch (IOException e) {
                    showDesktopItemError(batch, batch.accumulatedText, mood, e);
                    throw e;
                }
            }
        } else if (part.type == Message.Type.Keyword) {
            doKeyword(batch, part);
        } else if (part.type == Message.Type.Delay) {
            doDelay(part);
        } else if (part.type == Message.Type.Item) {
            batch.accumulatedText.add(part);
        } else if (part.type == Message.Type.Text) {
            batch.accumulatedText.add(part);
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
        RenderDesktopItem renderDesktopItem = new RenderDesktopItem(teaseLib, resources, part.value);
        completeSectionAll();
        renderQueue.submit(renderDesktopItem);
    }

    private void showDesktopItemError(Batch batch, MessageTextAccumulator accumulatedText, String mood, IOException e)
            throws IOException, InterruptedException {
        accumulatedText.add(new MessagePart(Message.Type.Text, e.getMessage()));
        completeSectionAll();
        show(batch.actor, accumulatedText.toString(), mood, batch.displayImage);
        batch.renderer.startCompleted();
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

    // CORRECT
    private void show(Actor actor, String text, MessageTextAccumulator accumulatedText, String mood,
            String displayImage) throws IOException, InterruptedException {
        teaseLib.transcript.info(text);
        show(actor, accumulatedText.toString(), mood, displayImage);
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

    // COMPLETE
    private void show(Actor actor, String text, String mood, String displayImage)
            throws IOException, InterruptedException {
        logMoodToTranscript(actor, mood, displayImage);
        logImageToTranscript(actor, displayImage);
        show(text, displayImage);
    }

    private void logMoodToTranscript(Actor actor, String mood, String displayImage) {
        if (actor.images.contains(displayImage) && mood != Mood.Neutral) {
            teaseLib.transcript.info("mood = " + mood);
        }
    }

    private void logImageToTranscript(Actor actor, String displayImage) {
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

    // COMPLETE
    private void show(String text, String displayImage) throws IOException, InterruptedException {
        if (!Thread.currentThread().isInterrupted()) {
            teaseLib.host.show(getImageBytes(displayImage), text);
        }
    }

    // TODO Rename parameter
    private byte[] getImageBytes(String displayImage) throws IOException, InterruptedException {
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

    protected void handleIOException(IOException e) throws IOException {
        ExceptionUtil.handleIOException(e, teaseLib.config, logger);
    }

    private void doKeyword(Batch batch, MessagePart part) {
        String keyword = part.value;
        if (keyword == Message.ActorImage) {
            throw new IllegalStateException(keyword + " must be resolved in pre-parse");
        } else if (keyword == Message.NoImage) {
            throw new IllegalStateException(keyword + " must be resolved in pre-parse");
        } else if (keyword == Message.ShowChoices) {
            completeSectionMandatory();
            batch.renderer.mandatoryCompleted();
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

    // private static Interval getDelayMillis(String args) {
    // double[] argv = getDelayInterval(args);
    // if (argv.length == 1) {
    // int delay = (int) (argv[0] * 1000.0);
    // return new Interval(delay, delay);
    // } else {
    // return new Interval((int) (argv[0] * 1000.0), (int) (argv[1] * 1000.0));
    // }
    // }

    // @Override
    // public String toString() {
    // long delayMillis = 0;
    // MessageTextAccumulator text = new MessageTextAccumulator();
    // for (RenderedMessage message : messages) {
    // AbstractMessage paragraphs = message;
    // for (Iterator<MessagePart> it = paragraphs.iterator(); it.hasNext();) {
    // MessagePart part = it.next();
    // text.add(part);
    // if (part.type == Type.Text) {
    // delayMillis += TextToSpeech.getEstimatedSpeechDuration(part.value);
    // } else if (part.type == Type.Delay) {
    // delayMillis += getDelayMillis(part.value).start;
    // }
    // }
    // }
    // String messageText = text.toString().replace("\n", " ");
    // int length = 40;
    // return "delay~" + String.format("%.2f", (double) delayMillis / 1000) + " Message='"
    // + (messageText.length() > length ? messageText.substring(0, length) + "..." : messageText + "'");
    // }

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
