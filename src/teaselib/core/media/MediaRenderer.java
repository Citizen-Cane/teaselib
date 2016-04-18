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

    public interface Replay extends MediaRenderer {
        enum Position {
            /**
             * Replays the whole renderer. This is functionally equalent to
             * calling {@code render()}
             */
            FromStart,
            /**
             * Replays the renderer from the end of the mandatory part.
             * 
             * For a message renderer, this means that the last part of the
             * message is displayed and spoken.
             */
            FromMandatory,

            /**
             * Replays just the end of the renderer. In this case the renderer
             * should just display its final state, and not delay anything.
             * 
             * For a message renderer, this results in just displaying the last
             * visuals, e.g. image and text part, without speaking anything.
             */
            End
        }

        void replay(Position replayPosition);
    }
}
