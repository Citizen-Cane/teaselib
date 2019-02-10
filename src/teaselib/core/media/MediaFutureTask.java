package teaselib.core.media;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

class MediaFutureTask<T extends MediaRenderer.Threaded> implements Future<Void> {
    protected final T mediaRenderer;
    private final Future<Void> future;

    MediaFutureTask(T mediaRenderer, Future<Void> future) {
        this.mediaRenderer = mediaRenderer;
        this.future = future;
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        return future.cancel(true);
    }

    @Override
    public boolean isCancelled() {
        return future.isCancelled() && medaiRendererIsUnblocked();
    }

    @Override
    public boolean isDone() {
        return future.isDone() && medaiRendererIsUnblocked();
    }

    private boolean medaiRendererIsUnblocked() {
        return mediaRenderer.hasCompletedStart() && mediaRenderer.hasCompletedMandatory()
                && mediaRenderer.hasCompletedAll();
    }

    @Override
    public Void get() throws InterruptedException, ExecutionException {
        return future.get();
    }

    @Override
    public Void get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        return future.get(timeout, unit);
    }
}