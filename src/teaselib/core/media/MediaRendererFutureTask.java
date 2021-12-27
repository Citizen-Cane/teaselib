package teaselib.core.media;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import teaselib.core.media.MediaRenderer.Threaded;

final class MediaRendererFutureTask implements Future<Object> {

    private final Threaded mediaRenderer;
    private final Future<?> future;

    MediaRendererFutureTask(Threaded mediaRenderer, Future<?> future) {
        this.mediaRenderer = mediaRenderer;
        this.future = future;
    }

    @Override
    public boolean isCancelled() {
        return future.isCancelled();
    }

    @Override
    public boolean isDone() {
        return future.isDone();
    }

    @Override
    public Object get() throws InterruptedException, ExecutionException {
        return future.get();
    }

    @Override
    public Object get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        return future.get(timeout, unit);
    }

    @Override
    public boolean cancel(boolean interrupt) {
        boolean cancel;
        if (mediaRenderer instanceof MediaRendererThread t) {
            if (!t.hasCompletedStart()) {
                // cancelled before started
                t.startCompleted();
                t.mandatoryCompleted();
                t.allCompleted();
                cancel = false;
            } else if (t.hasCompletedAll()) {
                // already cancelled
                cancel = false;
            } else {
                cancel = future.cancel(interrupt);
            }
        } else {
            cancel = future.cancel(interrupt);
        }
        return cancel;
    }

}
