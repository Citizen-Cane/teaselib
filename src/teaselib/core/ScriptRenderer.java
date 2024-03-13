package teaselib.core;

import static java.util.Collections.singletonList;
import static java.util.concurrent.TimeUnit.HOURS;
import static teaselib.core.concurrency.NamedExecutorService.newUnlimitedThreadPool;
import static teaselib.core.concurrency.NamedExecutorService.singleThreadedQueue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import teaselib.Actor;
import teaselib.Message;
import teaselib.Replay;
import teaselib.Replay.Position;
import teaselib.core.ScriptEventArgs.BeforeMessage;
import teaselib.core.ScriptEventArgs.BeforeMessage.OutlineType;
import teaselib.core.debug.CheckPoint;
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

    final MediaRendererQueue renderQueue = new MediaRendererQueue();
    private final ExecutorService scriptFunctionExecutor = newUnlimitedThreadPool("Script task", 1, HOURS);
    private final ExecutorService inputMethodExecutor = newUnlimitedThreadPool("Input method", 1, HOURS);
    private final ExecutorService prefetchExecutor = singleThreadedQueue("Image prefetch", 1, HOURS);

    private final List<MediaRenderer> queuedRenderers = new ArrayList<>();
    private final List<MediaRenderer.Threaded> backgroundRenderers = new ArrayList<>();

    private List<MediaRenderer> playedRenderers = null;
    private final List<Message> prependedMessages = new ArrayList<>();
    private Actor currentActor = null;

    final SectionRenderer sectionRenderer;
    final ScriptEventInputMethod scriptEventInputMethod;

    public final ScriptEvents events;
    public final AudioSync audioSync;

    ScriptRenderer(TeaseLib teaseLib) {
        this.sectionRenderer = new SectionRenderer(teaseLib, new MediaRendererQueue(renderQueue));
        this.scriptEventInputMethod = new ScriptEventInputMethod(inputMethodExecutor);
        this.events = new ScriptEvents(scriptEventInputMethod);
        this.audioSync = sectionRenderer.textToSpeechPlayer.audioSync;
    }

    public Actor currentActor() {
        return currentActor;
    }

    @Override
    public void close() {
        var actor = currentActor;
        if (actor != null) {
            var textToSpeechPlayer = sectionRenderer.textToSpeechPlayer;
            if (textToSpeechPlayer != null && textToSpeechPlayer.hasTTSVoice(actor)) {
                textToSpeechPlayer.stop(actor);
            }
        }
        scriptFunctionExecutor.shutdown();
        inputMethodExecutor.shutdown();
        prefetchExecutor.shutdown();
        renderQueue.getExecutorService().shutdown();
        sectionRenderer.close();
    }

    void awaitStartCompleted() throws InterruptedException {
        renderQueue.awaitStartCompleted();
    }

    void awaitMandatoryCompleted() throws InterruptedException {
        renderQueue.awaitMandatoryCompleted();
    }

    void awaitAllCompleted() throws InterruptedException {
        renderQueue.awaitAllCompleted();
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

    public boolean hasCompletedMandatory() {
        return renderQueue.hasCompletedMandatory();
    }

    void renderIntertitle(TeaseLib teaseLib, Message message, Decorator[] decorators) throws InterruptedException {
        var composed = composeIntertitleMessage(message);
        var interTitle = new RenderInterTitle(RenderedMessage.of(composed, decorators), teaseLib);
        sectionRenderer.setRenderer(interTitle);
        renderMessage(teaseLib, message.actor, interTitle, OutlineType.NewSection);
    }

    private Message composeIntertitleMessage(Message message) {
        Message composed;
        if (hasPrependedMessages()) {
            prependedMessages.add(message);
            composed = new Message(message.actor);
            prependedMessages.stream().flatMap(Message::stream).filter(part -> part.isAnyOf(Message.Type.TextTypes))
                    .forEach(composed::add);
            prependedMessages.clear();
        } else {
            composed = message;
        }
        return composed;
    }

    Replay getReplay() {
        return new ReplayImpl(this, playedRenderers);
    }

    void replay(List<MediaRenderer> renderers, Replay.Position position) {
        renderQueue.replay(renderers, position);
        playedRenderers = renderers;
    }

    void prependMessage(Message message) {
        prependedMessages.add(message);
    }

    void renderPrependedMessages(TeaseLib teaseLib, ResourceLoader resources, Actor actor, Decorator[] decorators)
            throws InterruptedException {
        List<Message> prepended = new ArrayList<>(prependedMessages);
        prependedMessages.clear();
        startMessages(teaseLib, resources, actor, prepended, decorators);
    }

    boolean hasPrependedMessages() {
        return !prependedMessages.isEmpty();
    }

    void startMessage(TeaseLib teaseLib, ResourceLoader resources, Message message, Decorator[] decorators)
            throws InterruptedException {
        startMessages(teaseLib, resources, message.actor, singletonList(message), decorators);
    }

    // TODO run method of media renderer should start rendering
    // -> currently it's started when say() is called

    void startMessages(TeaseLib teaseLib, ResourceLoader resources, Actor actor, List<Message> messages,
            Decorator[] decorators) throws InterruptedException {
        List<RenderedMessage> renderedMessages = convertMessagesToRendered(messages, decorators);
        var messageRenderer = sectionRenderer.createStartBatch(actor, renderedMessages, resources);
        sectionRenderer.setRenderer(messageRenderer);
        renderMessage(teaseLib, actor, messageRenderer, OutlineType.NewSection);
    }

    void appendMessage(TeaseLib teaseLib, ResourceLoader resources, Actor actor, Message message,
            Decorator[] decorators) throws InterruptedException {
        if (!prependedMessages.isEmpty()) {
            startMessage(teaseLib, resources, message, decorators);
        } else {
            List<RenderedMessage> renderedMessages = convertMessagesToRendered(singletonList(message), decorators);
            appendMessagesToSection(teaseLib, actor, renderedMessages);
        }
    }

    private void appendMessagesToSection(TeaseLib teaseLib, Actor actor, List<RenderedMessage> messages)
            throws InterruptedException {
        synchronized (renderQueue.activeRenderers) {
            synchronized (queuedRenderers) {
                awaitStartCompleted();
                boolean playing = sectionRenderer.append(messages);
                if (!playing) {
                    awaitAllCompleted();
                    List<MediaRenderer> replay = new ArrayList<>(playedRenderers);
                    sectionRenderer.restoreCurrentRenderer(replay);
                    set(actor);
                    fireBeforeMessageEvent(teaseLib, OutlineType.AppendParagraph);
                    replay(replay, Position.FromCurrentPosition);
                } else {
                    awaitMandatoryCompleted();
                    set(actor);
                    fireBeforeMessageEvent(teaseLib, OutlineType.AppendParagraph);
                }
            }
        }
    }

    void replaceMessage(TeaseLib teaseLib, ResourceLoader resources, Actor actor, Message message,
            Decorator[] decorators) throws InterruptedException {
        replaceMessage(teaseLib, resources, actor, singletonList(message), decorators);
    }

    private void replaceMessage(TeaseLib teaseLib, ResourceLoader resources, Actor actor, List<Message> messages,
            Decorator[] decorators)
            throws InterruptedException {
        List<RenderedMessage> renderedMessages = convertMessagesToRendered(messages, decorators);
        var messageRenderer = sectionRenderer.createBatch(actor, renderedMessages, resources);
        sectionRenderer.setRenderer(messageRenderer);
        renderMessage(teaseLib, actor, messageRenderer, OutlineType.ReplaceParagraph);
    }

    private List<RenderedMessage> convertMessagesToRendered(List<Message> messages, Decorator[] decorators) {
        Stream<Message> all = Stream.concat(prependedMessages.stream(), messages.stream());
        List<RenderedMessage> renderedMessages = all.map(message -> RenderedMessage.of(message, decorators)).toList();
        prependedMessages.clear();
        return renderedMessages;
    }

    private void renderMessage(TeaseLib teaseLib, Actor actor, MediaRenderer messageRenderer, OutlineType outlineType)
            throws InterruptedException {

        synchronized (renderQueue.activeRenderers) {
            synchronized (queuedRenderers) {
                queueRenderer(messageRenderer);
                // Remember this set for replay
                // TODO remember the outline type for this set to ensure proper section/paragraph pauses
                playedRenderers = new ArrayList<>(queuedRenderers);

                // Remember in order to clear queued before completing previous set
                List<MediaRenderer> nextSet = new ArrayList<>(queuedRenderers);

                // clear queue for next set before completing the current set,
                // to ensure the next set is empty when the current set is cancelled
                queuedRenderers.clear();

                // Now the current set can be completed, and canceling the
                // current set will result in an empty next set
                awaitAllCompleted();
                // Start a new chapter in the transcript
                teaseLib.transcript.info("");

                set(actor);
                fireBeforeMessageEvent(teaseLib, outlineType);
                renderQueue.start(nextSet);
            }
            startBackgroundRenderers();
            awaitStartCompleted();
        }
    }

    boolean haveMultipleParagraphs() {
        return sectionRenderer.hasMultipleParagraphs();
    }

    void set(Actor actor) {
        if (actor != currentActor) {
            currentActor = actor;
            events.actorChanged.fire(new ScriptEventArgs.ActorChanged(currentActor));
        }
    }

    private void fireBeforeMessageEvent(TeaseLib teaseLib, BeforeMessage.OutlineType outlineType) {
        teaseLib.checkPointReached(CheckPoint.Script.NewMessage);
        events.beforeMessage.fire(new ScriptEventArgs.BeforeMessage(outlineType));
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
        backgroundRenderers.stream().filter(Threaded::hasCompletedAll).toList().stream()
                .forEach(backgroundRenderers::remove);
    }

    void startBackgroundRenderer(MediaRenderer.Threaded renderer) {
        renderQueue.submit(renderer);
    }

    void stopBackgroundRenderers() {
        synchronized (backgroundRenderers) {
            renderQueue.cancel(backgroundRenderers, Predicate.not(MediaRenderer.Threaded::hasCompletedAll));
            backgroundRenderers.clear();
        }
    }

    ExecutorService getScriptFunctionExecutorService() {
        return scriptFunctionExecutor;
    }

    ExecutorService getInputMethodExecutorService() {
        return inputMethodExecutor;
    }

    public ExecutorService getPrefetchExecutorService() {
        return prefetchExecutor;
    }

}
