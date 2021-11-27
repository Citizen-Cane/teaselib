package teaselib.core.media;

public interface MediaRenderer extends Runnable {
    public interface Threaded extends MediaRenderer {
        /**
         * Wait for the renderer having completed the introduction phase of its content
         */
        void awaitStartCompleted();

        /**
         * Wait for the renderer having completed the mandatory part of its content
         */
        void awaitMandatoryCompleted();

        /**
         * Wait for the renderer to render all its content
         */
        void awaitAllCompleted();

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
}
