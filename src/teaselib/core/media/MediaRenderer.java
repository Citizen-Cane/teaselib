package teaselib.core.media;

import java.io.IOException;

public interface MediaRenderer {
    public void render() throws IOException;

    public interface Threaded extends MediaRenderer {

        /**
         * Wait for the renderer having completed the introduction phase of its
         * content
         */
        public void completeStart();

        /**
         * Wait for the renderer having completed the mandatory part of its
         * content
         */
        public void completeMandatory();

        /**
         * Wait for the renderer to render all its content
         */
        public void completeAll();

        /**
         * Determine whether the renderer has completed its start.
         */
        public boolean hasCompletedStart();

        /**
         * Determine whether the renderer has completed its mandatory part.
         */
        public boolean hasCompletedMandatory();

        /**
         * Determine whether the renderer has completed all.
         */
        public boolean hasCompletedAll();

        /**
         * End the render thread.
         */
        public void interrupt();

        /**
         * Instruct the render to end rendering, this might include waiting
         * until the renderer has finished up rendering its visuals, like
         * intro/outro sequences
         */
        public void join();
    }
}
