package teaselib.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import teaselib.Message;
import teaselib.Replay;
import teaselib.Replay.Position;
import teaselib.core.media.MediaRenderer;
import teaselib.core.media.MediaRenderer.Threaded;
import teaselib.core.media.MediaRendererQueue;
import teaselib.core.media.RenderInterTitle;
import teaselib.core.media.RenderMessage;
import teaselib.core.media.RenderedMessage;
import teaselib.core.media.RenderedMessage.Decorator;
import teaselib.core.texttospeech.TextToSpeechPlayer;

/**
 * @author Citizen-Cane
 *
 */
public class ScriptRenderer {
    private static final Logger logger = LoggerFactory.getLogger(ScriptRenderer.class);

    private final MediaRendererQueue renderQueue = new MediaRendererQueue();
    private final List<MediaRenderer> queuedRenderers = new ArrayList<>();
    private final List<MediaRenderer.Threaded> backgroundRenderers = new ArrayList<>();

    private List<MediaRenderer> playedRenderers = null;
    private final List<Message> prependedMessages = new ArrayList<>();
    private RenderMessage renderMessage = null;

    ScriptRenderer() {
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
        stopBackgroundRenderers();
    }

    void renderIntertitle(TeaseLib teaseLib, Message message) {
        if (!prependedMessages.isEmpty()) {
            throw new IllegalStateException("renderIntertitle doesn't support prepended messages");
        }

        RenderInterTitle interTitle = new RenderInterTitle(message, teaseLib);
        renderMessage(teaseLib, interTitle);
    }

    public class ReplayImpl implements Replay {
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

    void replayFromCurrentPosition() {
        if (renderMessage.hasCompletedMandatory()) {
            renderMessage.completeAll();
            renderMessage.replay(Position.FromCurrentPosition);
            renderQueue.submit(renderMessage);
        } else {
            throw new IllegalStateException("Can only replay afer completing mandatory " + renderMessage);
        }
    }

    void prependMessage(Message message) {
        renderMessage = null;
        prependedMessages.add(message);
    }

    void renderMessage(TeaseLib teaseLib, ResourceLoader resources, Message message, Decorator[] decorators,
            Optional<TextToSpeechPlayer> textToSpeech) {
        List<RenderedMessage> messages = new ArrayList<>(prependedMessages.size() + 1);
        prependedMessages.stream().forEach(prepended -> messages.add(RenderedMessage.of(prepended, decorators)));
        prependedMessages.clear();
        messages.add(RenderedMessage.of(message, decorators));
        // Actor is eventually used for logging and transscript but nothing else
        // TODO Decide whether to remove parameter?
        renderMessage = new RenderMessage(teaseLib, renderQueue, resources, textToSpeech, message.actor, messages);
        renderMessage(teaseLib, renderMessage);
    }

    void appendMessage(TeaseLib teaseLib, ResourceLoader resources, Message message, Decorator[] decorators,
            Optional<TextToSpeechPlayer> textToSpeech) {
        if (renderMessage == null) {
            renderMessage(teaseLib, resources, message, decorators, textToSpeech);
        } else {
            if (!renderMessage.append(RenderedMessage.of(message, decorators))) {
                replayFromCurrentPosition();
            }
        }
    }

    void replaceMessage(TeaseLib teaseLib, ResourceLoader resources, Message message, Decorator[] decorators,
            Optional<TextToSpeechPlayer> textToSpeech) {
        if (renderMessage == null) {
            renderMessage(teaseLib, resources, message, decorators, textToSpeech);
        } else {
            if (!renderMessage.replace(RenderedMessage.of(message, decorators))) {
                replayFromCurrentPosition();
            }
        }
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
                // Now the current set can be completed, and canceling the
                // current set will result in an empty next set
                completeAll();

                // Start a new message in the log
                teaseLib.transcript.info("");
                renderQueue.start(nextSet);
            }
            startBackgroundRenderers();
            renderQueue.completeStarts();
        }
    }

    protected void queueRenderers(List<MediaRenderer> renderers) {
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

    ExecutorService getExecutorService() {
        return renderQueue.getExecutorService();
    }
}
