package teaselib.core.media;

import java.util.ArrayList;
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
import teaselib.core.ScriptInterruptedException;
import teaselib.core.concurrency.NamedExecutorService;
import teaselib.core.media.MessageRendererQueue.Batch;
import teaselib.core.util.ExceptionUtil;

public class MediaRendererQueue {
    private static final Logger logger = LoggerFactory.getLogger(MediaRendererQueue.class);

    static final String RenderTaskBaseName = "RenderTask ";

    private final Map<MediaRenderer.Threaded, Future<?>> activeRenderers = new HashMap<>();
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
     * @param activeRenderers
     *            The renderers to start.
     * @param teaseLib
     */
    public void start(List<MediaRenderer> newSet) {
        synchronized (activeRenderers) {
            if (!activeRenderers.isEmpty()) {
                throw new IllegalStateException();
            }

            play(newSet);
        }
    }

    public void replay(List<MediaRenderer> renderers, Replay.Position replayPosition) {
        synchronized (activeRenderers) {
            completeAll();
            endAll();

            if (!activeRenderers.isEmpty()) {
                throw new IllegalStateException();
            }

            List<MediaRenderer> replayable = new ArrayList<>();
            for (MediaRenderer r : renderers) {
                if (r instanceof ReplayableMediaRenderer || replayPosition == Replay.Position.FromStart) {
                    ((ReplayableMediaRenderer) r).replay(replayPosition);
                    replayable.add(r);
                }
            }

            play(replayable);
        }
    }

    private void play(List<MediaRenderer> mediaRenderers) {
        for (MediaRenderer r : mediaRenderers) {
            // TODO Resolve interfaces - Batch has its own thread already
            if (r instanceof MediaRenderer.Threaded) {
                submit((MediaRenderer.Threaded) r);
            } else {
                r.run();
            }
        }
    }

    public void completeStarts() {
        synchronized (activeRenderers) {
            if (!activeRenderers.isEmpty()) {
                logger.debug("Completing all threaded renderers starts");
                for (MediaRenderer.Threaded renderer : activeRenderers.keySet()) {
                    renderer.completeStart();
                }
            } else {
                logger.debug("Threaded Renderers completeStarts : queue empty");
            }
        }
    }

    public void completeMandatories() {
        synchronized (activeRenderers) {
            if (!activeRenderers.isEmpty()) {
                logger.debug("Completing all threaded renderers mandatory part");
                for (MediaRenderer.Threaded renderer : activeRenderers.keySet()) {
                    renderer.completeMandatory();
                }
            } else {
                logger.debug("Threaded Renderers completeMandatories : queue empty");
            }
        }
    }

    /**
     * Completes rendering of all currently running renderers. Returns after all renderers that running at the start of
     * this method have been finished.
     */
    public void completeAll() {
        synchronized (activeRenderers) {
            if (!activeRenderers.isEmpty()) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Completing all threaded renderers {}", activeRenderers);
                }
                for (MediaRenderer.Threaded renderer : activeRenderers.keySet()) {
                    renderer.completeAll();
                }
            } else if (logger.isDebugEnabled()) {
                logger.debug("Threaded Renderers completeAll: queue empty");
            }
        }
    }

    /**
     * Ends rendering of all currently running renderers. Each renderer thread is interrupted and should end as soon as
     * possible.
     */
    public void endAll() {
        synchronized (activeRenderers) {
            if (!activeRenderers.isEmpty()) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Ending all threaded renderers");
                }

                List<Future<?>> futures = new ArrayList<>();
                RuntimeException exception = null;
                for (MediaRenderer.Threaded renderer : new ArrayList<>(activeRenderers.keySet())) {
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

                if (exception != null) {
                    throw exception;
                } else if (!activeRenderers.isEmpty()) {
                    throw new IllegalStateException();
                }
            } else {
                if (logger.isDebugEnabled()) {
                    logger.debug("Threaded Renderers endAll: queue empty");
                }
            }
        }
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
            // TODO Must be managed by named executor service
            // setThreadName(nameForActiveThread());
            Future<?> future;
            // TODO Batch renderer facade and ThreadedMediaRendererhave duplicated code -> merge
            // TODO encapsulate this instanceof branches into the referenced classes
            if (mediaRenderer instanceof Batch.RendererFacade) {
                mediaRenderer.run();
                future = ((Batch.RendererFacade) mediaRenderer).getTask();
            } else if (mediaRenderer instanceof MediaRendererThread) {
                future = new MediaFutureTask<MediaRendererThread>((MediaRendererThread) mediaRenderer,
                        (Future<Void>) executor.submit(mediaRenderer)) {
                    @Override
                    public boolean cancel(boolean mayInterruptIfRunning) {
                        boolean cancel = super.cancel(mayInterruptIfRunning);
                        mediaRenderer.startCompleted();
                        mediaRenderer.mandatoryCompleted();
                        mediaRenderer.allCompleted();
                        return cancel;
                    }
                };
            } else {
                future = executor.submit(mediaRenderer);
            }
            activeRenderers.put(mediaRenderer, future);
            return future;
        }
    }

    public Future<?> interrupt(MediaRenderer.Threaded mediaRenderer) {
        Future<?> future = activeRenderers.remove(mediaRenderer);
        if (future == null) {
            throw new IllegalArgumentException(mediaRenderer.toString());
        } else {
            if (!future.isDone() && !future.isCancelled()) {
                future.cancel(true);
            } else {
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

    private static void join(Future<?> future) {
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

    public ExecutorService getExecutorService() {
        return executor;
    }
}
