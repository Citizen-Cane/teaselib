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
    SelfBondage_TimeLock,
    Key_Safe,
    Computer_Controlled_EStim,
    Computer_Controlled_Vibrator,
    Webcam

    ;

    public static final Gadgets[] All = { SelfBondage_TimeLock, Key_Safe, Computer_Controlled_Vibrator,
            Gadgets.Computer_Controlled_EStim, Webcam };

    public static final Gadgets[][] Categories = { All };
}

// TODO Make up opinion on whether to replace this with auto-connecting devices
// at any time - would be perfectly possible after each message when no script
// function is active.
