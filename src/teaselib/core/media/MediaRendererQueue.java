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
import java.util.function.Predicate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import teaselib.Replay;
import teaselib.core.concurrency.NamedExecutorService;
import teaselib.core.util.ExceptionUtil;

public class MediaRendererQueue {

    private static final Logger logger = LoggerFactory.getLogger(MediaRendererQueue.class);

    static final String RenderTaskBaseName = "RenderTask ";

    public final Map<MediaRenderer.Threaded, Future<?>> activeRenderers = new HashMap<>();
    private final ExecutorService executor;

    public MediaRendererQueue() {
        executor = NamedExecutorService.newUnlimitedThreadPool(RenderTaskBaseName, 1, TimeUnit.HOURS);
    }

    public MediaRendererQueue(MediaRendererQueue mediaRendererQueue) {
        this.executor = mediaRendererQueue.executor;
    }

    /**
     * Start a batch of renderers. This is reentrant and called from multiple threads. Frequent users are the main
     * script thread and script function threads.
     * 
     * The functions waits for running renderers to complete, then starts the renderers suppplied in {@code renderers}.
     * 
     */
    public void start(List<MediaRenderer> newSet) {
        synchronized (activeRenderers) {
            checkPreviousSetCompleted();
            play(newSet);
        }
    }

    public void replay(List<MediaRenderer> renderers, Replay.Position position) {
        synchronized (activeRenderers) {
            var replayable = replayable(renderers);
            if (position == Replay.Position.FromCurrentPosition) {
                set(replayable, position);
                replayable.stream().forEach(renderer -> {
                    submit(renderer);
                });
            } else {
                endAll();
                set(replayable, position);
                if (position == Replay.Position.FromStart) {
                    play(renderers);
                } else {
                    replayable.stream().forEach(renderer -> {
                        submit(renderer);
                    });
                }
            }
        }

    }

    private static void set(List<ReplayableMediaRenderer> replayable, Replay.Position position) {
        for (ReplayableMediaRenderer r : replayable) {
            r.set(position);
        }
    }

    private void checkPreviousSetCompleted() {
        if (!activeRenderers.isEmpty()) {
            throw new IllegalStateException("Previous render-set not completed");
        }
    }

    private static List<ReplayableMediaRenderer> replayable(List<? extends MediaRenderer> renderers) {
        return renderers.stream()
                .filter(ReplayableMediaRenderer.class::isInstance)
                .map(ReplayableMediaRenderer.class::cast)
                .toList();
    }

    private void play(List<? extends MediaRenderer> mediaRenderers) {
        for (MediaRenderer r : mediaRenderers) {
            if (r instanceof MediaRenderer.Threaded threaded) {
                submit(threaded);
            } else {
                r.run();
            }
        }
    }

    public void awaitStartCompleted() throws InterruptedException {
        synchronized (activeRenderers) {
            if (!activeRenderers.isEmpty()) {
                logger.debug("Completing all threaded renderers starts");
                for (MediaRenderer.Threaded renderer : activeRenderers.keySet()) {
                    renderer.awaitStartCompleted();
                }
            } else {
                logger.debug("Threaded Renderers completeStarts : queue empty");
            }
        }
    }

    public void awaitMandatoryCompleted() throws InterruptedException {
        synchronized (activeRenderers) {
            if (!activeRenderers.isEmpty()) {
                logger.debug("Completing all threaded renderers mandatory part");
                for (MediaRenderer.Threaded renderer : activeRenderers.keySet()) {
                    renderer.awaitMandatoryCompleted();
                }
            } else {
                logger.debug("Threaded Renderers completeMandatories : queue empty");
            }
        }
    }

    /**
     * Completes rendering of all currently running renderers. Returns after all renderers that running at the start of
     * this method have been finished.
     * 
     * @throws InterruptedException
     */
    public void awaitAllCompleted() throws InterruptedException {
        synchronized (activeRenderers) {
            if (activeRenderers.isEmpty()) {
                logger.debug("Threaded Renderers completeAll: queue empty");
            } else {
                if (logger.isDebugEnabled()) {
                    logger.debug("Completing all threaded renderers {}", activeRenderers);
                }

                for (MediaRenderer.Threaded renderer : activeRenderers.keySet()) {
                    renderer.awaitAllCompleted();
                }

                List<Future<?>> futures = drainActiveRenderers();
                join(futures);

                if (!activeRenderers.isEmpty()) {
                    throw new IllegalStateException();
                }
            }
        }
    }

    /**
     * Ends rendering of all currently running renderers. Each renderer thread is interrupted and should end as soon as
     * possible.
     */
    public void endAll() {
        synchronized (activeRenderers) {
            if (activeRenderers.isEmpty()) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Threaded Renderers endAll: queue empty");
                }
            } else {
                if (logger.isDebugEnabled()) {
                    logger.debug("Ending all threaded renderers");
                }

                List<Future<?>> futures = drainActiveRenderers();
                cancel(futures);
                try {
                    join(futures);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }

                if (!activeRenderers.isEmpty()) {
                    throw new IllegalStateException();
                }
            }
        }
    }

    private List<Future<?>> drainActiveRenderers() {
        return drain(activeRenderers.keySet());
    }

    private List<Future<?>> drain(Collection<MediaRenderer.Threaded> elements) {
        List<Future<?>> futures = new ArrayList<>();
        for (MediaRenderer.Threaded renderer : new ArrayList<>(elements)) {
            futures.add(activeRenderers.remove(renderer));
        }
        return futures;
    }

    public boolean hasCompletedStarts() {
        synchronized (activeRenderers) {
            if (!activeRenderers.isEmpty()) {
                for (MediaRenderer.Threaded renderer : activeRenderers.keySet()) {
                    if (!renderer.hasCompletedStart()) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    public boolean hasCompletedMandatory() {
        synchronized (activeRenderers) {
            if (!activeRenderers.isEmpty()) {
                for (MediaRenderer.Threaded renderer : activeRenderers.keySet()) {
                    if (!renderer.hasCompletedMandatory()) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    public boolean hasCompletedAll() {
        synchronized (activeRenderers) {
            if (!activeRenderers.isEmpty()) {
                for (MediaRenderer.Threaded renderer : activeRenderers.keySet()) {
                    if (!renderer.hasCompletedAll()) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    public Future<?> submit(MediaRenderer.Threaded mediaRenderer) {
        synchronized (activeRenderers) {
            Future<?> task = executor.submit(mediaRenderer);
            Future<?> future = new MediaRendererFutureTask(mediaRenderer, task);
            activeRenderers.put(mediaRenderer, future);
            return future;
        }
    }

    public List<Future<?>> cancel(List<MediaRenderer.Threaded> mediaRenderers,
            Predicate<MediaRenderer.Threaded> matching) {
        synchronized (activeRenderers) {
            List<Future<?>> futures = drain(mediaRenderers.stream().filter(matching).toList());
            cancel(futures);
            return futures;
        }
    }

    public Future<?> cancel(MediaRenderer.Threaded mediaRenderer) {
        synchronized (activeRenderers) {
            Future<?> future = activeRenderers.remove(mediaRenderer);
            return cancel(future);
        }
    }

    private void cancel(List<Future<?>> futures) {
        futures.stream().forEach(this::cancel);
    }

    private Future<?> cancel(Future<?> future) {
        if (!future.isDone() && !future.isCancelled()) {
            future.cancel(true);
        }
        return future;
    }

    private static void join(List<Future<?>> futures) throws InterruptedException {
        RuntimeException exception = null;
        InterruptedException interrupted = null;

        for (Future<?> future : futures) {
            try {
                join(future);
            } catch (InterruptedException e) {
                interrupted = e;
            } catch (RuntimeException e) {
                if (exception == null) {
                    exception = e;
                }
            }
        }

        if (interrupted != null) {
            throw interrupted;
        } else if (exception != null) {
            throw exception;
        }
    }

    private static void join(Future<?> future) throws InterruptedException {
        try {
            if (!future.isCancelled() || !future.isDone()) {
                future.get();
            }
        } catch (ExecutionException e) {
            throw ExceptionUtil.asRuntimeException(ExceptionUtil.reduce(e));
        }
    }

    public ExecutorService getExecutorService() {
        return executor;
    }

}
