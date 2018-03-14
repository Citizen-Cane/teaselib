package teaselib.core.media;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import teaselib.Replay;
import teaselib.core.ScriptInterruptedException;
import teaselib.core.media.MediaRenderer.Threaded;

public class MediaRendererQueue {
    private static final Logger logger = LoggerFactory.getLogger(MediaRendererQueue.class);

    private final HashMap<Class<?>, MediaRenderer.Threaded> threadedMediaRenderers = new HashMap<>();

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
                if (r instanceof MediaRenderer.Threaded) {
                    threadedMediaRenderers.put(r.getClass(), (MediaRenderer.Threaded) r);
                }
                try {
                    r.render();
                } catch (ScriptInterruptedException e) {
                    throw e;
                } catch (Exception e) {
                    logger.error(e.getMessage(), e);
                }
            }
        }
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
                    ((ReplayableMediaRenderer) r).replay(replayPosition);
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
                logger.debug("Completing all threaded renderers " + renderers);
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
                // Interrupt them all
                RuntimeException exception = null;
                for (MediaRenderer.Threaded renderer : threadedMediaRenderers.values()) {
                    try {
                        renderer.interrupt();
                    } catch (RuntimeException e) {
                        exception = e;
                    }
                }
                if (exception != null) {
                    throw exception;
                }
                try {
                    // then wait for them to complete
                    for (MediaRenderer.Threaded renderer : threadedMediaRenderers.values()) {
                        renderer.join();
                    }
                } catch (RuntimeException e) {
                    exception = e;
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
}
