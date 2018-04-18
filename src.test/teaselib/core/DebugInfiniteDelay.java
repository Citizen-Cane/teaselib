package teaselib.core;

import teaselib.core.media.MediaRendererThread;

class DebugInfiniteDelay extends MediaRendererThread {
    public DebugInfiniteDelay(TeaseLib teaseLib) {
        super(teaseLib);
    }

    @Override
    protected void renderMedia() throws InterruptedException {
        startCompleted();
        mandatoryCompleted();
        allCompleted();

        synchronized (this) {
            while (!Thread.currentThread().isInterrupted()) {
                wait();
            }
        }
    }
}