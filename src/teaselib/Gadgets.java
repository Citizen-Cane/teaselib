package teaselib;

/**
 * Devices supported by TeaseLib. These should be configurable by the user, so scripts can check what's available.
 * <p>
 * 
 * 
 * @author Citizen-Cane
 *
 */
public enum Gadgets {
    Key_Release,
    Key_Safe,
    EStim_Controller,
    Vibrator_Controller,
    Webcam

    ;

    public static final Gadgets[] All = { Key_Release, Key_Safe, Vibrator_Controller,
            Gadgets.EStim_Controller, Webcam };

    public static final Gadgets[][] Categories = { All };
}

// TODO Make up opinion on whether to replace this with auto-connecting devices
// at any time - would be perfectly possible after each message when no script
// function is active.
