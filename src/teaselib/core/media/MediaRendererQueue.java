package teaselib.core.media;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import teaselib.Replay;
import teaselib.Replay.Position;
import teaselib.core.ScriptInterruptedException;
import teaselib.core.concurrency.NamedExecutorService;
import teaselib.core.media.MediaRenderer.Threaded;
import teaselib.core.util.ExceptionUtil;

public class MediaRendererQueue {
    private static final Logger logger = LoggerFactory.getLogger(MediaRendererQueue.class);

    static final String RenderTaskBaseName = "RenderTask ";

    // TODO Remove threadedMediaRenderers and throw if not empty when next set is played
    Map<MediaRenderer.Threaded, Future<?>> interruptableRenderers = new HashMap<>();

    // TODO remove
    private final HashMap<Class<?>, MediaRenderer.Threaded> threadedMediaRenderers = new HashMap<>();
    private final ExecutorService executor = NamedExecutorService.newUnlimitedThreadPool(RenderTaskBaseName, 1,
            TimeUnit.HOURS);

    /**
     * Start a batch of renderers. This is reentrant and called from multiple threads. Frequent users are the main
     * script thread and script function threads.
     * 
     * The functions waits for running renderers to complete, then starts the renderers suppplied in {@code renderers}.
     * 
     * @param renderers
     *            The renderers to start.
     * @param teaseLib
     */
    public void start(Collection<MediaRenderer> renderers) {
        synchronized (threadedMediaRenderers) {
            threadedMediaRenderers.clear();
            for (MediaRenderer r : renderers) {
                try {
                    if (r instanceof MediaRenderer.Threaded) {
                        runThreadedRenderer((MediaRenderer.Threaded) r);
                    } else {
                        r.run();
                    }
                } catch (ScriptInterruptedException e) {
                    throw e;
                } catch (Exception e) {
                    // TODO use ExceptionUtil and config to stop on Render Error
                    logger.error(e.getMessage(), e);
                }
            }
        }
    }

    private void runThreadedRenderer(MediaRenderer.Threaded r) {
        threadedMediaRenderers.put(r.getClass(), r);
        submit(r);
    }

    public void replay(Collection<MediaRenderer> renderers, Replay.Position replayPosition) {
        synchronized (threadedMediaRenderers) {
            completeAll();
            endAll();
            threadedMediaRenderers.clear();
            for (MediaRenderer r : renderers) {
                if (r instanceof ReplayableMediaRenderer || replayPosition == Replay.Position.FromStart) {
                    if (r instanceof MediaRenderer.Threaded) {
                        threadedMediaRenderers.put(r.getClass(), (MediaRenderer.Threaded) r);
                    }
                    replay(((ReplayableMediaRenderer) r), replayPosition);
                }
            }
        }
    }

    private Map<Class<?>, Threaded> getThreadedRenderers() {
        synchronized (threadedMediaRenderers) {
            return new HashMap<>(threadedMediaRenderers);
        }
    }

    public void completeStarts() {
        Map<Class<?>, Threaded> renderers = getThreadedRenderers();
        if (!renderers.isEmpty()) {
            logger.debug("Completing all threaded renderers starts");
            for (MediaRenderer.Threaded renderer : renderers.values()) {
                renderer.completeStart();
            }
        } else {
            logger.debug("Threaded Renderers completeStarts : queue empty");
        }
    }

    public void completeMandatories() {
        Map<Class<?>, Threaded> renderers = getThreadedRenderers();
        if (!renderers.isEmpty()) {
            logger.debug("Completing all threaded renderers mandatory part");
            for (MediaRenderer.Threaded renderer : renderers.values()) {
                renderer.completeMandatory();
            }
        } else {
            logger.debug("Threaded Renderers completeMandatories : queue empty");
        }
    }

    /**
     * Completes rendering of all currently running renderers. Returns after all renderers that running at the start of
     * this method have been finished.
     */
    public void completeAll() {
        Map<Class<?>, Threaded> renderers = getThreadedRenderers();
        if (!renderers.isEmpty()) {
            if (logger.isDebugEnabled()) {
                logger.debug("Completing all threaded renderers {}", renderers);
            }
            for (MediaRenderer.Threaded renderer : renderers.values()) {
                renderer.completeAll();
            }
        } else if (logger.isDebugEnabled()) {
            logger.debug("Threaded Renderers completeAll: queue empty");
        }
    }

    /**
     * Ends rendering of all currently running renderers. Each renderer thread is interrupted and should end as soon as
     * possible.
     */
    public void endAll() {
        synchronized (threadedMediaRenderers) {
            if (!threadedMediaRenderers.isEmpty()) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Ending all threaded renderers");
                }

                List<Future<?>> futures = new ArrayList<>();
                RuntimeException exception = null;
                try {
                    for (MediaRenderer.Threaded renderer : new ArrayList<>(threadedMediaRenderers.values())) {
                        try {
                            futures.add(interrupt(renderer));
                        } catch (RuntimeException e) {
                            exception = e;
                        }
                    }
                    if (exception != null) {
                        throw exception;
                    }
                    try {
                        for (Future<?> future : futures) {
                            join(future);
                        }
                    } catch (RuntimeException e) {
                        exception = e;
                    }
                } finally {
                    threadedMediaRenderers.clear();
                }

                if (exception != null) {
                    throw exception;
                }
            } else {
                if (logger.isDebugEnabled()) {
                    logger.debug("Threaded Renderers endAll: queue empty");
                }
            }
        }
    }

    public boolean hasCompletedStarts() {
        Map<Class<?>, Threaded> renderers = getThreadedRenderers();
        if (!renderers.isEmpty()) {
            for (MediaRenderer.Threaded renderer : renderers.values()) {
                if (!renderer.hasCompletedStart()) {
                    return false;
                }
            }
        }
        return true;
    }

    public boolean hasCompletedMandatory() {
        Map<Class<?>, Threaded> renderers = getThreadedRenderers();
        if (!renderers.isEmpty()) {
            for (MediaRenderer.Threaded renderer : renderers.values()) {
                if (!renderer.hasCompletedMandatory()) {
                    return false;
                }
            }
        }
        return true;
    }

    public boolean hasCompletedAll() {
        Map<Class<?>, Threaded> renderers = getThreadedRenderers();
        if (!renderers.isEmpty()) {
            for (MediaRenderer.Threaded renderer : renderers.values()) {
                if (!renderer.hasCompletedAll()) {
                    return false;
                }
            }
        }
        return true;
    }

    public Future<?> submit(MediaRenderer.Threaded mediaRenderer) {
        synchronized (interruptableRenderers) {
            // TODO Must be managed by named executor service
            // setThreadName(nameForActiveThread());
            Future<?> future = submit((Runnable) mediaRenderer);
            interruptableRenderers.put(mediaRenderer, future);
            return future;
        }
    }

    public Future<?> interrupt(MediaRenderer.Threaded mediaRenderer) {
        Future<?> future = interruptableRenderers.remove(mediaRenderer);
        if (future == null) {
            throw new IllegalArgumentException(mediaRenderer.toString());
        } else {
            if (!future.isDone() && !future.isCancelled()) {
                future.cancel(true);
            }
            return future;
        }
        // TODO Must be managed by named executor service
        // setThreadName(nameForSleepingThread());
    }

    public void interruptAndJoin(MediaRenderer.Threaded mediaRenderer) {
        join(interrupt(mediaRenderer));
    }

    public void join(MediaRenderer.Threaded mediaRenderer) {
        Future<?> future;
        synchronized (interruptableRenderers) {
            future = interruptableRenderers.remove(mediaRenderer);
        }

        if (future == null) {
            throw new IllegalArgumentException(mediaRenderer.toString());
        } else {
            join(future);
        }
    }

    public void join(Future<?> future) {
        try {
            if (!future.isCancelled() || !future.isDone()) {
                future.get();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ScriptInterruptedException(e);
        } catch (ExecutionException e) {
            throw ExceptionUtil.asRuntimeException(ExceptionUtil.reduce(e));
        }
    }

    public void replay(ReplayableMediaRenderer renderer, Position position) {
        // TODO Avoid replaying if still rendering
        submit(() -> renderer.replay(position));
    }

    public Future<?> submit(Runnable runnable) {
        return executor.submit(runnable);
    }
}
