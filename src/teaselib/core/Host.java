package teaselib.core;

import java.io.File;
import java.util.List;

import teaselib.core.ui.InputMethod;

/**
 * Render target decouples the lib from the actual implementaiton of the host. The interface is closely modeled to the
 * SexSCripts ISCript interface See http://ss.deviatenow.com/viewtopic.php?f=4&t=2
 * 
 * @author someone
 *
 */
public interface Host {
    Audio audio(ResourceLoader resources, String path);

    /**
     * Show text and image. Since text and image determine the layout, they must be set simultanously.
     * 
     * @param image
     * @param text
     */
    void show(byte[] imageBytes, String text);

    void showInterTitle(String text);

    // @Deprecated
    // void showButton(String message);

    /**
     * Show a list of textboxes. The key of all of the maps ued here is the name value to change.
     * 
     * @param message
     *            Message to be displayed, or null if none
     * @param labels
     * @param values
     * @return
     */
    List<Boolean> showCheckboxes(String caption, List<String> choices, List<Boolean> values, boolean allowCancel);

    InputMethod inputMethod();

    /**
     * Install a handler being executed when the user quits the application or the main script is otherwise interrupted.
     * The quit handler should be short, as the closing process cannot be cancelled. A final message and setting a flag
     * to punish next time should be sufficient.
     * 
     * The script is just executed once, to prevent repeated execution, and to allow the application finally to quit. It
     * is a wise idea to set flags immediately, just in case.
     * 
     * @param onQuitHandler
     *            The script to execute when the slave quits the application.
     */
    void setQuitHandler(Runnable onQuitHandler);

    VideoRenderer getDisplay(VideoRenderer.Type displayType);

    enum Location {
        TeaseLib,
        Host,
        User,
        Log
    }

    File getLocation(Location folder);
}
