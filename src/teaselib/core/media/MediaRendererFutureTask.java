package teaselib.core.media;

import java.util.concurrent.Future;

import teaselib.core.concurrency.AbstractFuture;
import teaselib.core.media.MediaRenderer.Threaded;

final class MediaRendererFutureTask extends AbstractFuture<Void> {

    private final Threaded mediaRenderer;

    @SuppressWarnings("unchecked")
    MediaRendererFutureTask(Threaded mediaRenderer, Future<?> future) {
        super((Future<Void>) future);
        this.mediaRenderer = mediaRenderer;
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
