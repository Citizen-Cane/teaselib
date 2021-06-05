package teaselib.core;

import static java.util.concurrent.TimeUnit.HOURS;
import static java.util.stream.Collectors.toList;
import static teaselib.core.concurrency.NamedExecutorService.newUnlimitedThreadPool;
import static teaselib.core.concurrency.NamedExecutorService.singleThreadedQueue;

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
import teaselib.Message.Type;
import teaselib.Replay;
import teaselib.core.ScriptEventArgs.BeforeNewMessage;
import teaselib.core.ScriptEventArgs.BeforeNewMessage.OutlineType;
import teaselib.core.debug.CheckPoint;
import teaselib.core.functional.TriFunction;
import teaselib.core.media.MediaRenderer;
import teaselib.core.media.MediaRenderer.Threaded;
import teaselib.core.media.MediaRendererQueue;
import teaselib.core.media.RenderInterTitle;
import teaselib.core.media.RenderedMessage;
import teaselib.core.media.RenderedMessage.Decorator;
import teaselib.core.media.SectionRenderer;

/**
 * @author Citizen-Cane
 *
 */
public class ScriptRenderer implements Closeable {
    static final Logger logger = LoggerFactory.getLogger(ScriptRenderer.class);

    private final MediaRendererQueue renderQueue = new MediaRendererQueue();
    private final ExecutorService scriptFunctionExecutor = newUnlimitedThreadPool("Script task", 1, HOURS);
    private final ExecutorService inputMethodExecutor = newUnlimitedThreadPool("Input method", 1, HOURS);
    private final ExecutorService prefetchExecutor = singleThreadedQueue("Image prefetch", 1, HOURS);

    private final List<MediaRenderer> queuedRenderers = new ArrayList<>();
    private final List<MediaRenderer.Threaded> backgroundRenderers = new ArrayList<>();

    private List<MediaRenderer> playedRenderers = null;
    private final List<Message> prependedMessages = new ArrayList<>();
    private Actor currentActor = null;
    private Message currentMessage = null;

    public final ScriptEvents events;

    final SectionRenderer sectionRenderer;
    final ScriptEventInputMethod scriptEventInputMethod;

    public final AudioSync audioSync;

    ScriptRenderer(TeaseLib teaseLib) {
        this.sectionRenderer = new SectionRenderer(teaseLib, new MediaRendererQueue(renderQueue));
        scriptEventInputMethod = new ScriptEventInputMethod(inputMethodExecutor);
        this.events = new ScriptEvents(scriptEventInputMethod);
        this.audioSync = sectionRenderer.textToSpeechPlayer.audioSync;
    }

    public Actor currentActor() {
        return currentActor;
    }

    @Override
    public void close() {
        scriptFunctionExecutor.shutdown();
        inputMethodExecutor.shutdown();
        renderQueue.getExecutorService().shutdown();
        sectionRenderer.close();
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
        // TODO decide whether background renderers should be stoppable manually
        // - > additional flexibility
        stopBackgroundRenderers();
    }

    boolean hasCompletedMandatory() {
        return renderQueue.hasCompletedMandatory();
    }

    void renderIntertitle(TeaseLib teaseLib, Message message, Decorator[] decorators) {
        Message composed;
        if (hasPrependedMessages()) {
            composed = new Message(currentActor);
            prependedMessages.add(message);
            prependedMessages.stream().flatMap(Message::stream).filter(part -> part.type == Type.Text)
                    .forEach(composed::add);
            prependedMessages.clear();
        } else {
            composed = message;
        }

        var interTitle = new RenderInterTitle(RenderedMessage.of(composed, decorators), teaseLib);
        renderMessage(teaseLib, message.actor, interTitle);
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

        // TODO run method of media renderer should start rendering
        // -> currently it's started when say() is called

        // TODO submitting new renderer internally -> old will be executed to end
        // - original idea was to append to the existing if continue if possible,
        // but in that case we can't return the original media renderer interface
        // -> only return the batch, don't run yet -> queue in batch.run()
        // TODO render section delay in queue, so it's not part of the paragraph anymore

        // Workaround: keep it for now, renderer is started and queued, waited for and ended

        renderMessage(teaseLib, resources, actor, messages, decorators, //
                sectionRenderer::say, BeforeNewMessage.OutlineType.NewSection);
    }

    public interface ConcatFunction extends TriFunction<Actor, List<RenderedMessage>, ResourceLoader, MediaRenderer> { //
    }

    void appendMessage(TeaseLib teaseLib, ResourceLoader resources, Actor actor, Message message,
            Decorator[] decorators) {
        if (!prependedMessages.isEmpty()) {
            renderMessage(teaseLib, resources, message, decorators);
        } else {
            renderMessage(teaseLib, resources, actor, Collections.singletonList(message), decorators, //
                    sectionRenderer::append, BeforeNewMessage.OutlineType.AppendParagraph);
        }
    }

    void replaceMessage(TeaseLib teaseLib, ResourceLoader resources, Actor actor, Message message,
            Decorator[] decorators) {
        renderMessage(teaseLib, resources, actor, Collections.singletonList(message), decorators, //
                sectionRenderer::replace, BeforeNewMessage.OutlineType.ReplaceParagraph);
    }

    private void renderMessage(TeaseLib teaseLib, ResourceLoader resources, Actor actor, List<Message> messages,
            Decorator[] decorators, ConcatFunction concatFunction, OutlineType outlineType) {
        fireNewMessageEvent(teaseLib, actor, outlineType);
        List<RenderedMessage> renderedMessages = convertMessagesToRendered(messages, decorators);
        MediaRenderer replaced = concatFunction.apply(actor, renderedMessages, resources);
        renderMessage(teaseLib, actor, replaced);
        currentMessage = messages.get(messages.size() - 1);
    }

    boolean isShowingInstructionalImage() {
        var current = currentMessage;
        return current != null && current.contains(Message.Type.Image);
    }

    boolean isInterTitle() {
        return playedRenderers != null && playedRenderers.stream().anyMatch(RenderInterTitle.class::isInstance);
    }

    void showAll(double delaySeconds) {
        sectionRenderer.showAll(delaySeconds);
    }

    private void fireNewMessageEvent(TeaseLib teaseLib, Actor actor, BeforeNewMessage.OutlineType outlineType) {
        teaseLib.checkPointReached(CheckPoint.Script.NewMessage);
        if (actor != currentActor) {
            currentActor = actor;
            events.actorChanged.fire(new ScriptEventArgs.ActorChanged(currentActor));
        }
        events.beforeMessage.fire(new ScriptEventArgs.BeforeNewMessage(outlineType));
    }

    private List<RenderedMessage> convertMessagesToRendered(List<Message> messages, Decorator[] decorators) {
        Stream<Message> all = Stream.concat(prependedMessages.stream(), messages.stream());
        List<RenderedMessage> renderedMessages = all.map(message -> RenderedMessage.of(message, decorators))
                .collect(toList());
        prependedMessages.clear();
        return renderedMessages;
    }

    private void renderMessage(TeaseLib teaseLib, Actor actor, MediaRenderer renderMessage) {
        synchronized (renderQueue.activeRenderers) {
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

                // Now the current set can be completed, and canceling the
                // current set will result in an empty next set
                completeAll();
                // Start a new chapter in the transcript
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

    ExecutorService getPrefetchExecutorService() {
        return prefetchExecutor;
    }

}
