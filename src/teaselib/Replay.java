package teaselib;

/**
 * @author Citizen-Cane
 *
 */
public interface Replay {
    enum Position {
        /**
         * Replays the whole renderer. This is functionally equivalent to
         * calling {@code render()}
         */
        FromStart,
        /**
         * Replays the renderer from the end of the mandatory part.
         * 
         * For a message renderer, this means that the last part of the message
         * is displayed and spoken.
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