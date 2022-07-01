package teaselib.core;

import static java.util.Collections.*;
import static java.util.concurrent.TimeUnit.*;
import static teaselib.core.concurrency.NamedExecutorService.*;

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

    void startMessages(TeaseLib teaseLib, ResourceLoader resources, Actor actor, List<Message> messages,
            Decorator[] decorators) throws InterruptedException {

        // TODO run method of media renderer should start rendering
        // -> currently it's started when say() is called

        // TODO submitting new renderer internally -> old will be executed to end
        // - original idea was to append to the existing if continue if possible,
        // but in that case we can't return the original media renderer interface
        // -> only return the batch, don't run yet -> queue in batch.run()
        // TODO render section delay in queue, so it's not part of the paragraph anymore

        // Workaround: keep it for now, renderer is started and queued, waited for and ended

        renderMessages(teaseLib, resources, actor, messages, decorators, //
                sectionRenderer::say, BeforeMessage.OutlineType.NewSection);
    }

    void appendMessage(TeaseLib teaseLib, ResourceLoader resources, Actor actor, Message message,
            Decorator[] decorators) throws InterruptedException {
        if (!prependedMessages.isEmpty()) {
            startMessage(teaseLib, resources, message, decorators);
        } else {
            List<RenderedMessage> renderedMessages = convertMessagesToRendered(singletonList(message), decorators);
            appendMessagesToSection(actor, renderedMessages);
        }
    }

    void replaceMessage(TeaseLib teaseLib, ResourceLoader resources, Actor actor, Message message,
            Decorator[] decorators) throws InterruptedException {
        renderMessages(teaseLib, resources, actor, singletonList(message), decorators, //
                sectionRenderer::replace, BeforeMessage.OutlineType.ReplaceParagraph);
    }

    void showAll(TeaseLib teaseLib, ResourceLoader resources, Actor actor, Message message, Decorator[] decorators)
            throws InterruptedException {
        renderMessages(teaseLib, resources, actor, singletonList(message), decorators, //
                sectionRenderer::showAll, BeforeMessage.OutlineType.ReplaceParagraph);
    }

    private void renderMessages(TeaseLib teaseLib, ResourceLoader resources, Actor actor, List<Message> messages,
            Decorator[] decorators, SectionRenderer.ConcatFunction concatFunction, OutlineType outlineType)
            throws InterruptedException {
        List<RenderedMessage> renderedMessages = convertMessagesToRendered(messages, decorators);
        MediaRenderer messageRenderer = concatFunction.apply(actor, renderedMessages, resources);
        renderMessage(teaseLib, actor, messageRenderer, outlineType);
    }

    boolean showsMultipleParagraphs() {
        return sectionRenderer.showsMultipleParagraphs();
    }

    boolean showsActorImage() {
        var current = sectionRenderer.lastParagraph();
        if (current != null) {
            var image = current.findLast(Message.Type.Image);
            if (image == null) {
                return false;
            } else if (currentActor.images.contains(image.value)) {
                return true;
            } else if (Message.NoImage.equalsIgnoreCase(image.value)) {
                return false;
            } else if (Message.ActorImage.equalsIgnoreCase(image.value)) {
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    boolean showsInstructionalImage() {
        var current = sectionRenderer.lastParagraph();
        if (current != null) {
            var image = current.findLast(Message.Type.Image);
            if (image == null) {
                return false;
            } else if (currentActor.images.contains(image.value)) {
                return false;
            } else if (Message.NoImage.equalsIgnoreCase(image.value)) {
                return false;
            } else if (Message.ActorImage.equalsIgnoreCase(image.value)) {
                return false;
            } else {
                return true;
            }
        } else {
            return false;
        }
    }

    boolean isInterTitle() {
        return playedRenderers != null && playedRenderers.stream().anyMatch(RenderInterTitle.class::isInstance);
    }

    private List<RenderedMessage> convertMessagesToRendered(List<Message> messages, Decorator[] decorators) {
        Stream<Message> all = Stream.concat(prependedMessages.stream(), messages.stream());
        List<RenderedMessage> renderedMessages = all.map(message -> RenderedMessage.of(message, decorators)).toList();
        prependedMessages.clear();
        return renderedMessages;
    }

    private void renderMessage(TeaseLib teaseLib, Actor actor, MediaRenderer messageRenderer, OutlineType outlineType)
            throws InterruptedException {
        remember(actor);

        fireBeforeMessageEvent(teaseLib, actor, outlineType);

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

                sectionRenderer.nextOutlineType = outlineType;
                // Now the current set can be completed, and canceling the
                // current set will result in an empty next set
                awaitAllCompleted();
                // Start a new chapter in the transcript
                teaseLib.transcript.info("");
                renderQueue.start(nextSet);
            }
            startBackgroundRenderers();
            renderQueue.awaitStartCompleted();
        }
    }

    // Used to define delay at the end of a section - before the beginning of a new section
    // - look-ahead : either inject delay between paragraphs or before a new section
    // + say(() starts playing a batch:
    // + while playing append() copies & modifies the messages and starts a new batch at the "current"
    // message
    // + the batch ends with a paragraphs delay or a section delay

    // Better:
    // + one batch per say()
    // +-- append rendered messages
    // +-- replace last message and modify current message index
    // -> usually continues rendering automatically
    // when completed re-start batch at current position

    // TODO prevent batch from running multiple times
    // + append has appneded new messages here
    // + either batch should continue, and nextOutlineType is wrong/obsolelte
    // + or batch has finished here and can be restarted after await...()
    // TODO must awaitCmpleteMandatory() before adding since we guarantee that
    // : render*() returns as soon as the new message has been started
    // -> extra rule for message renderer

    // Multiple renderers, single message list?
    // - does each renderer extend?
    // - What does replay() do?
    // -> Replay replays the last set of renderers
    // + up to now replay() is used after recognitionRejected-Script to restore the showAll() state
    // -> The last set is replayed from Position.Mandatory or Position.End

    // TODO define what global renderer settings do
    // + delay: delay for render set
    // + sound etc. : plays at start of set
    // -> replay multiple messages and sounds from start does not work
    // because messages are appended via a new set

    //
    // Final solution:
    //

    // + say() & append() must work the same way as say(...)
    // ->
    // + play() to current limit, no matter what has been appended
    // + append() appends to current message renderer
    // + append() waits until the message renderer has played to its limit
    // + append() re-plays() the current set from the current position
    // -> message renderer continues execution, other renderers
    // ---- continue execution as well or
    // ---- are already finished
    // queueRenderer can return because the current set continues execution
    //
    // render implementation
    // + at the end of the limit, when there are no more appended entries,
    // --- message renderer executes section delay (does not append)
    // + append appends paragraph delay when message renderer has not completed mandatory
    // + append ends section delay when replaying

    private void appendMessagesToSection(Actor actor, List<RenderedMessage> messages)
            throws InterruptedException {
        remember(actor);

        synchronized (renderQueue.activeRenderers) {
            synchronized (queuedRenderers) {
                boolean playing = sectionRenderer.append(messages);
                if (!playing) {
                    awaitAllCompleted();
                    List<MediaRenderer> replay = new ArrayList<>(playedRenderers);
                    replay(replay, Position.FromCurrentPosition);
                } else {
                    awaitMandatoryCompleted();
                }
            }
        }
    }

    private void remember(Actor actor) {
        if (actor != currentActor) {
            currentActor = actor;
            events.actorChanged.fire(new ScriptEventArgs.ActorChanged(currentActor));
        }
    }

    private void fireBeforeMessageEvent(TeaseLib teaseLib, Actor actor, BeforeMessage.OutlineType outlineType) {
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
