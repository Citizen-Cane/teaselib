package teaselib;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import teaselib.util.Select;

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

    public static final Select.Statement All = Select.items(Key_Release, Key_Safe, Vibrator_Controller,
            Gadgets.EStim_Controller, Webcam);

    public static final List<Select.Statement> Categories = Collections.unmodifiableList(Arrays.asList(All));
}

// TODO Make up opinion on whether to replace this with auto-connecting devices
// at any time - would be perfectly possible after each message when no script
// function is active.
