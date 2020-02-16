package teaselib.core;

import static java.util.Collections.singletonList;
import static java.util.concurrent.TimeUnit.HOURS;
import static java.util.stream.Collectors.toList;
import static teaselib.core.concurrency.NamedExecutorService.newUnlimitedThreadPool;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import teaselib.Actor;
import teaselib.Message;
import teaselib.Replay;
import teaselib.core.debug.CheckPoint;
import teaselib.core.media.MediaRenderer;
import teaselib.core.media.MediaRenderer.Threaded;
import teaselib.core.media.MediaRendererQueue;
import teaselib.core.media.MessageRendererQueue;
import teaselib.core.media.RenderInterTitle;
import teaselib.core.media.RenderedMessage;
import teaselib.core.media.RenderedMessage.Decorator;

/**
 * @author Citizen-Cane
 *
 */
public class ScriptRenderer implements Closeable {
    static final Logger logger = LoggerFactory.getLogger(ScriptRenderer.class);

    final MediaRendererQueue renderQueue = new MediaRendererQueue();
    private final ExecutorService scriptFunctionExecutor = newUnlimitedThreadPool("Script task", 1, HOURS);
    private final ExecutorService inputMethodExecutor = newUnlimitedThreadPool("Input method", 1, HOURS);

    final List<MediaRenderer> queuedRenderers = new ArrayList<>();
    private final List<MediaRenderer.Threaded> backgroundRenderers = new ArrayList<>();

    List<MediaRenderer> playedRenderers = null;
    private final List<Message> prependedMessages = new ArrayList<>();

    final MessageRendererQueue messageRenderer;

    public final ScriptEvents events;

    ScriptRenderer(TeaseLib teaseLib) {
        this.messageRenderer = new MessageRendererQueue(teaseLib, new MediaRendererQueue(renderQueue));
        this.events = new ScriptEvents(new ScriptEventInputMethod(inputMethodExecutor));
    }

    @Override
    public void close() {
        scriptFunctionExecutor.shutdown();
        inputMethodExecutor.shutdown();
        renderQueue.getExecutorService().shutdown();
        messageRenderer.close();
    }

    void completeStarts() {
        renderQueue.completeStarts();
    }

    void completeMandatory() {
        renderQueue.completeMandatories();
    }

    /**
     * Just wait for everything to be rendered (messages displayed, sounds played, delay expired), and continue
     * execution of the script.
     * <p>
     * This won't display a button, it just waits. Background threads will continue to run.
     */
    void completeAll() {
        renderQueue.completeAll();
        renderQueue.endAll();
    }

    /**
     * Stop rendering and end all render threads
     */
    void endAll() {
        renderQueue.endAll();
        // TODO decide whether background renderers should be stopped manually
        // - > additional flexibility
        stopBackgroundRenderers();
    }

    boolean hasCompletedMandatory() {
        return renderQueue.hasCompletedMandatory();
    }

    void renderIntertitle(TeaseLib teaseLib, Message message) {
        if (hasPrependedMessages()) {
            throw new IllegalStateException("renderIntertitle doesn't support prepended messages");
        }

        RenderInterTitle interTitle = new RenderInterTitle(message, teaseLib);
        renderMessage(teaseLib, interTitle);
    }

    class ReplayImpl implements Replay {
        final List<MediaRenderer> renderers;

        public ReplayImpl(List<MediaRenderer> renderers) {
            super();
            logger.info("Remembering renderers in replay {}", this);
            this.renderers = new ArrayList<>(renderers);
        }

        @Override
        public void replay(Replay.Position replayPosition) {
            synchronized (queuedRenderers) {
                logger.info("Replaying renderers from replay {}", this);
                // Finish current set before replaying
                completeMandatory();
                // Restore the prompt that caused running the SR-rejected script
                // as soon as possible
                endAll();
                renderQueue.replay(renderers, replayPosition);
                playedRenderers = renderers;
            }
        }
    }

    Replay getReplay() {
        return new ReplayImpl(playedRenderers);
    }

    void prependMessage(Message message) {
        prependedMessages.add(message);
    }

    void renderPrependedMessages(TeaseLib teaseLib, ResourceLoader resources, Actor actor, Decorator[] decorators) {
        renderMessages(teaseLib, resources, actor, Collections.emptyList(), decorators);
    }

    boolean hasPrependedMessages() {
        return !prependedMessages.isEmpty();
    }

    void renderMessage(TeaseLib teaseLib, ResourceLoader resources, Message message, Decorator[] decorators) {
        renderMessages(teaseLib, resources, message.actor, Collections.singletonList(message), decorators);
    }

    void renderMessages(TeaseLib teaseLib, ResourceLoader resources, Actor actor, List<Message> messages,
            Decorator[] decorators) {
        List<RenderedMessage> renderedMessages = convertMessagesToRendered(messages, decorators);

        // TODO run method of media renderer should start rendering
        // -> currently it's started when say() is called

        // TODO submitting new renderer internally -> old will be executed to end
        // - original idea was to append to the existing if continue if possible,
        // but in that case we can't return the original media renderer interface
        // -> only return the batch, don't run yet -> queue in batch.run()
        // TODO render section delay in queue, so it's not part of the paragraph anymore

        // Workaround: keep it for now, renderer is started and queued
        // waited for and ended
        MediaRenderer say = messageRenderer.say(actor, renderedMessages, resources);
        renderMessage(teaseLib, say);
    }

    void appendMessage(TeaseLib teaseLib, ResourceLoader resources, Actor actor, Message message,
            Decorator[] decorators) {
        List<RenderedMessage> renderedMessages = convertMessagesToRendered(singletonList(message), decorators);
        MediaRenderer say = messageRenderer.append(actor, renderedMessages, resources);
        renderMessage(teaseLib, say);
    }

    void replaceMessage(TeaseLib teaseLib, ResourceLoader resources, Actor actor, Message message,
            Decorator[] decorators) {
        List<RenderedMessage> renderedMessages = convertMessagesToRendered(singletonList(message), decorators);
        MediaRenderer say = messageRenderer.replace(actor, renderedMessages, resources);
        renderMessage(teaseLib, say);
    }

    private List<RenderedMessage> convertMessagesToRendered(List<Message> messages, Decorator[] decorators) {
        Stream<Message> all = Stream.concat(prependedMessages.stream(), messages.stream());
        List<RenderedMessage> renderedMessages = all.map(message -> RenderedMessage.of(message, decorators))
                .collect(toList());
        prependedMessages.clear();
        return renderedMessages;
    }

    private void renderMessage(TeaseLib teaseLib, MediaRenderer renderMessage) {
        synchronized (renderQueue) {
            synchronized (queuedRenderers) {
                queueRenderer(renderMessage);
                // Remember this set for replay
                playedRenderers = new ArrayList<>(queuedRenderers);
                // Remember in order to clear queued before completing previous set
                List<MediaRenderer> nextSet = new ArrayList<>(queuedRenderers);
                // Must clear queue for next set before completing current,
                // because if the current set is cancelled,
                // the next set must be discarded
                queuedRenderers.clear();

                completeMandatory();
                events.beforeMessage.run(new ScriptEventArgs());

                // Now the current set can be completed, and canceling the
                // current set will result in an empty next set
                completeAll();
                teaseLib.checkPointReached(CheckPoint.Script.NewMessage);

                // Start a new message in the log
                teaseLib.transcript.info("");
                renderQueue.start(nextSet);
            }
            startBackgroundRenderers();
            renderQueue.completeStarts();
        }
    }

    void queueRenderers(List<MediaRenderer> renderers) {
        synchronized (queuedRenderers) {
            queuedRenderers.addAll(renderers);
        }
    }

    public void queueRenderer(MediaRenderer renderer) {
        synchronized (queuedRenderers) {
            queuedRenderers.add(renderer);
        }
    }

    public void queueBackgroundRenderer(MediaRenderer.Threaded renderer) {
        synchronized (backgroundRenderers) {
            backgroundRenderers.add(renderer);
        }
    }

    void startBackgroundRenderers() {
        synchronized (backgroundRenderers) {
            cleanupCompletedBackgroundRenderers();
            backgroundRenderers.stream().filter(t -> !t.hasCompletedStart()).forEach(this::startBackgroundRenderer);
        }
    }

    void cleanupCompletedBackgroundRenderers() {
        backgroundRenderers.stream().filter(Threaded::hasCompletedAll).collect(Collectors.toList()).stream()
                .forEach(backgroundRenderers::remove);
    }

    void startBackgroundRenderer(MediaRenderer.Threaded renderer) {
        renderQueue.submit(renderer);
    }

    void stopBackgroundRenderers() {
        synchronized (backgroundRenderers) {
            backgroundRenderers.stream().filter(t -> !t.hasCompletedAll()).forEach(renderQueue::interruptAndJoin);
            backgroundRenderers.clear();
        }
    }

    ExecutorService getScriptFunctionExecutorService() {
        return scriptFunctionExecutor;
    }

    ExecutorService getInputMethodExecutorService() {
        return inputMethodExecutor;
    }

}
