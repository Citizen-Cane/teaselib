package teaselib.core.media;

public interface MediaRenderer extends Runnable {

    public interface Threaded extends MediaRenderer {
        /**
         * Wait for the renderer having completed the introduction phase of its content
         */
        void awaitStartCompleted() throws InterruptedException;

        /**
         * Wait for the renderer having completed the mandatory part of its content
         */
        void awaitMandatoryCompleted() throws InterruptedException;

        /**
         * Wait for the renderer to render all its content
         */
        void awaitAllCompleted() throws InterruptedException;

        /**
         * Determine whether the renderer has completed its start.
         */
        boolean hasCompletedStart();

        /**
         * Determine whether the renderer has completed its mandatory part.
         */
        boolean hasCompletedMandatory();

        /**
         * Determine whether the renderer has completed all.
         */
        boolean hasCompletedAll();

    }

    static final Threaded None = new MediaRenderer.Threaded() {

        @Override
        public void run() { //
        }

        @Override
        public boolean hasCompletedStart() {
            return true;
        }

        @Override
        public boolean hasCompletedMandatory() {
            return true;
        }

        @Override
        public boolean hasCompletedAll() {
            return true;
        }

        @Override
        public void awaitStartCompleted() { //
        }

        @Override
        public void awaitMandatoryCompleted() { //
        }

        @Override
        public void awaitAllCompleted() { //
        }
    };
}
