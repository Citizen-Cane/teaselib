package teaselib;

import java.io.IOException;

/**
 * @author Citizen-Cane
 *
 */
public interface Replay {

    interface Replayable {
        void run() throws InterruptedException, IOException;
    }

    enum Position {
        /**
         * Replays the whole renderer. This is functionally equivalent to calling {@code render()}
         */
        FromStart,

        /**
         * Continues from the current position if paused or appended, or plays the end if completed
         */
        FromCurrentPosition,

        /**
         * Replays the renderer from the end of the mandatory part.
         * 
         * For a message renderer, this means that the last section of the message is displayed and spoken.
         */
        FromMandatory,

        /**
         * Replays just the end of the renderer. In this case the renderer should just display its final state, and not
         * delay anything.
         * 
         * For a message renderer, this results in just displaying the last visuals, e.g. image and text part, without
         * speaking anything.
         */
        End
    }

    void replay(Position replayPosition);

}