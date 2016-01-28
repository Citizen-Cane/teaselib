package teaselib.core;

import java.io.IOException;
import java.util.List;

import teaselib.core.events.Delegate;

/**
 * Render target decouples the lib from the actual implementaiton of the host.
 * The interface is closely modeled to the SexSCripts ISCript interface See
 * http://ss.deviatenow.com/viewtopic.php?f=4&t=2
 * 
 * @author someone
 *
 */
public interface Host {
    /**
     * Play the sound denoted by path and wait until it's finished. Sound stops
     * if the current thread is interrupted.
     * 
     * @param resources
     *            The resource loader to be used for loading the sound.
     * @param path
     *            The resource path to the wav, mp3 or ogg sound.
     */
    void playSound(ResourceLoader resources, String path) throws IOException;

    /**
     * Play the sound denoted by the resource path. Return immediately.
     * 
     * @param resources
     *            The resource loader to be used for loading the sound.
     * @param path
     *            The resource path to the wav, mp3 or ogg sound.
     * @return An opaque handle to the sound. Can be used to stop the sound.
     */
    Object playBackgroundSound(ResourceLoader resources, String path)
            throws IOException;

    /**
     * Stop the sound denoted by the resource path.
     * 
     * @param path
     *            The path to the sound resource.
     */
    void stopSound(Object handle);

    /**
     * Show text and image. Since text and image determine the layout, they must
     * be set simultanously.
     * 
     * @param image
     * @param text
     */
    void show(byte[] imageBytes, String text);

    // @Deprecated
    // void showButton(String message);

    /**
     * Show a list of textboxes. The key of all of the maps ued here is the name
     * value to change.
     * 
     * @param message
     *            Message to be displayed, or null if none
     * @param labels
     * @param values
     * @return
     */
    List<Boolean> showCheckboxes(String caption, List<String> choices,
            List<Boolean> values, boolean allowCancel);

    /**
     * Get a list of delegates, one for each user interaction. The index is the
     * same as for showInteraction.
     * 
     * @return List of delegates, run the appropriate delegate to perform the
     *         user interaction of the corresponding user interface element.
     */
    List<Delegate> getClickableChoices(List<String> choices);

    /**
     * Removes the current set of buttons and causes reply() to return
     * {@code null}.
     * 
     * @return true if the choices have been dismissed.
     */
    boolean dismissChoices(List<String> choices);

    /**
     * Choose one from a list of choices
     * 
     * @param choices
     *            The choices to display
     * @param timeout
     *            The timeout after which the buttons should disappear
     *            automatically
     * @return The index of the chosen item, or TeaseScript.TimeOut = -1 if no
     *         button was clicked
     * @exception ScriptInterruptedException
     *                If the thread is interrupted.
     */
    int reply(List<String> choices);

    /**
     * Install a handler being executed when the user quits the application or
     * the main script is otherwise interrupted. The quit handler should be
     * short, as the closing process cannot be cancelled. A final message and
     * setting a flag to punish next time should be sufficient.
     * 
     * The script is just executed once, to prevent repeated execution, and to
     * allow the application finally to quit. It is a wise idea to set flags
     * immediately, just in case.
     * 
     * @param onQuitHandler
     *            The script to execute when the slave quits the application.
     */
    void setQuitHandler(Runnable onQuitHandler);
}
