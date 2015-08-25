package teaselib.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import teaselib.TeaseLib;

public class MediaRendererQueue {

    protected final HashMap<Class<?>, MediaRenderer.Threaded> threadedMediaRenderers = new HashMap<Class<?>, MediaRenderer.Threaded>();

    public MediaRendererQueue() {
    }

    /**
     * Start multiple renderers at once. This is reentrant and caleld from
     * multiple threads. Prominent users are the main script thread and closure
     * threads. So don't block for too long, or stop button response may suffer
     * from long delays.
     * 
     * @param renderers
     * @param teaseLib
     */
    public void start(Collection<MediaRenderer> renderers, TeaseLib teaseLib) {
        synchronized (this) {
            for (MediaRenderer r : renderers) {
                start(r, teaseLib);
            }
        }
    }

    /**
     * Start a media renderer, but wait for other renderers of the same kind to
     * complete first
     * 
     * @param mediaMenderer
     * @param teaseScript
     */
    public void start(MediaRenderer mediaMenderer, TeaseLib teaseLib) {
        // Always call {@code Thread.interrupted()} before throwing a
        // {@ScriptInterruptedException}
        // in order to clear the interrupted state of the thread
        if (Thread.interrupted()) {
            throw new ScriptInterruptedException();
        }
        if (mediaMenderer instanceof MediaRenderer.Threaded) {
            // Before a media renderer can render, all predecessors must
            // complete their work
            Class<?> key = mediaMenderer.getClass();
            final MediaRenderer.Threaded renderer;
            synchronized (threadedMediaRenderers) {
                if (threadedMediaRenderers.containsKey(key)) {
                    renderer = threadedMediaRenderers.get(key);
                } else {
                    renderer = null;
                }
            }
            if (renderer != null) {
                renderer.completeAll();
            }
            synchronized (threadedMediaRenderers) {
                threadedMediaRenderers.put(mediaMenderer.getClass(),
                        (MediaRenderer.Threaded) mediaMenderer);
            }
            mediaMenderer.render(teaseLib);
        } else {
            // Just start immediately
            mediaMenderer.render(teaseLib);
        }
    }

    private Collection<MediaRenderer.Threaded> getMediaRenderersThreadSafe() {
        List<MediaRenderer.Threaded> copy;
        synchronized (threadedMediaRenderers) {
            copy = new ArrayList<MediaRenderer.Threaded>(
                    threadedMediaRenderers.values());
        }
        return copy;
    }

    public void completeStarts() {
        Collection<MediaRenderer.Threaded> renderers = getMediaRenderersThreadSafe();
        if (renderers.size() > 0) {
            for (MediaRenderer.Threaded renderer : renderers) {
                renderer.completeStart();
            }
        }
    }

    public void completeMandatories() {
        Collection<MediaRenderer.Threaded> renderers = getMediaRenderersThreadSafe();
        if (renderers.size() > 0) {
            for (MediaRenderer.Threaded renderer : renderers) {
                renderer.completeMandatory();
            }
        } else {
            TeaseLib.logDetail("Threaded Renderers queue: empty");
        }
    }

    /**
     * Completes rendering of all currently running renderers. Returns after all
     * renderers that running at the start of this method have been finished.
     * The method is not synchronized, so other threads may start new renderers
     * during the completion of the current batch.
     */
    public void completeAll() {
        // No additional synchronization needed here,
        // since a new renderer is started only
        // after the previous instance has finished
        Collection<MediaRenderer.Threaded> renderers = getMediaRenderersThreadSafe();
        if (renderers.size() > 0) {
            for (MediaRenderer.Threaded renderer : renderers) {
                renderer.completeAll();
            }
            // TODO New entries may have been added!
            synchronized (threadedMediaRenderers) {
                threadedMediaRenderers.clear();
            }
        } else {
            TeaseLib.logDetail("Threaded Renderers queue: empty");
        }
    }

    // TODO Locked on "stop", choose() hangs until Delay Renderer has completed
    // A closure thread calls completeAll() -> can enter endAll() only after the
    // completeAll() returns because they're both synchronized
    /**
     * Ends rendering of all currently running renderers. The renderer thread is
     * interrupted and should end as soon as possible.
     */
    public void endAll() {
        // No additional synchronization needed here,
        // since a new renderer is started only
        // after the previous instance has finished
        Collection<MediaRenderer.Threaded> renderers = getMediaRenderersThreadSafe();
        if (renderers.size() > 0) {
            for (MediaRenderer.Threaded renderer : renderers) {
                renderer.end();
            }
            // TODO New ones may have been added
            synchronized (threadedMediaRenderers) {
                threadedMediaRenderers.clear();
            }
        } else {
            TeaseLib.logDetail("Threaded Renderers queue: empty");
        }
    }
}
