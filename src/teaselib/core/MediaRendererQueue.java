package teaselib.core;

import java.util.Collection;
import java.util.HashMap;

import teaselib.TeaseLib;

public class MediaRendererQueue {

    private final HashMap<Class<?>, MediaRenderer.Threaded> threadedMediaRenderers = new HashMap<Class<?>, MediaRenderer.Threaded>();

    public MediaRendererQueue() {
    }

    /**
     * Start a batch of renderers. This is reentrant and called from multiple
     * threads. Frequent users are the main script thread and script function
     * threads.
     * 
     * The functions waits for running renderers to complete, then starts the
     * renderers suppplied in {@code renderers}.
     * 
     * @param renderers
     *            The renderers to start.
     * @param teaseLib
     */
    public void start(Collection<MediaRenderer> renderers, TeaseLib teaseLib) {
        completeAll();
        // At this point, all threaded media renderers have been completed,
        // and the threadedMediaRenderers list is empty
        synchronized (threadedMediaRenderers) {
            for (MediaRenderer r : renderers) {
                start(r, teaseLib);
            }
        }
    }

    /**
     * Start a single media renderer.
     * 
     * @param mediaMenderer
     */
    private void start(MediaRenderer mediaMenderer, TeaseLib teaseLib) {
        // Always call {@code Thread.interrupted()} before throwing a
        // {@ScriptInterruptedException}
        // in order to clear the interrupted state of the thread
        if (Thread.interrupted()) {
            throw new ScriptInterruptedException();
        }
        if (mediaMenderer instanceof MediaRenderer.Threaded) {
            threadedMediaRenderers.put(mediaMenderer.getClass(),
                    (MediaRenderer.Threaded) mediaMenderer);
            // Start render thread
            mediaMenderer.render(teaseLib);
        } else {
            // Render immediately
            mediaMenderer.render(teaseLib);
        }
    }

    public void completeStarts() {
        synchronized (threadedMediaRenderers) {
            if (threadedMediaRenderers.size() > 0) {
                TeaseLib.logDetail("Completing all threaded renderers starts");
                for (MediaRenderer.Threaded renderer : threadedMediaRenderers
                        .values()) {
                    renderer.completeStart();
                }
            } else {
                TeaseLib.logDetail("Threaded Renderers completeStarts : queue empty");
            }
        }
    }

    public void completeMandatories() {
        synchronized (threadedMediaRenderers) {
            if (threadedMediaRenderers.size() > 0) {
                TeaseLib.logDetail("Completing all threaded renderers mandatory part");
                for (MediaRenderer.Threaded renderer : threadedMediaRenderers
                        .values()) {
                    renderer.completeMandatory();
                }
            } else {
                TeaseLib.logDetail("Threaded Renderers completeMandatories : queue empty");
            }
        }
    }

    /**
     * Completes rendering of all currently running renderers. Returns after all
     * renderers that running at the start of this method have been finished.
     */
    public void completeAll() {
        synchronized (threadedMediaRenderers) {
            if (threadedMediaRenderers.size() > 0) {
                TeaseLib.logDetail("Completing all threaded renderers");
                for (MediaRenderer.Threaded renderer : threadedMediaRenderers
                        .values()) {
                    renderer.completeAll();
                }
                threadedMediaRenderers.clear();
            } else {
                TeaseLib.logDetail("Threaded Renderers completeAll: queue empty");
            }
        }
    }

    /**
     * Ends rendering of all currently running renderers. Each renderer thread
     * is interrupted and should end as soon as possible.
     */
    public void endAll() {
        synchronized (threadedMediaRenderers) {
            if (threadedMediaRenderers.size() > 0) {
                TeaseLib.logDetail("Ending all threaded renderers");
                // Interrupt them all
                for (MediaRenderer.Threaded renderer : threadedMediaRenderers
                        .values()) {
                    renderer.interrupt();
                }
                // then wait for them to complete
                for (MediaRenderer.Threaded renderer : threadedMediaRenderers
                        .values()) {
                    renderer.join();
                }
                threadedMediaRenderers.clear();
            } else {
                TeaseLib.logDetail("Threaded Renderers endAll: queue empty");
            }
        }
    }
}
