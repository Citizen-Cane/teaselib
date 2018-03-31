package teaselib.core;

import teaselib.core.media.MediaRendererThread;

class DebugInfiniteDelay extends MediaRendererThread {
    public DebugInfiniteDelay(TeaseLib teaseLib) {
        super(teaseLib);
    }

    @Override
    protected void renderMedia() throws InterruptedException {
        try {
            startCompleted();
            mandatoryCompleted();
            allCompleted();

            synchronized (this) {
                wait();
            }
        } catch (InterruptedException e) {
            // TODO Shouldn't be necessary
            super.completeAll();
            throw e;
        }
    }
}