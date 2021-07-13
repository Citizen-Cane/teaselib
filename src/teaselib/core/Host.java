package teaselib.core;

import java.awt.geom.Point2D;
import java.io.File;
import java.io.IOException;
import java.util.List;

import teaselib.core.ai.perception.HumanPose;
import teaselib.core.configuration.Configuration;
import teaselib.core.ui.InputMethod;
import teaselib.util.AnnotatedImage;

/**
 * Render target decouples the lib from the actual implementaiton of the host. The interface is closely modeled to the
 * SexSCripts ISCript interface See http://ss.deviatenow.com/viewtopic.php?f=4&t=2
 * 
 * @author Citizen-Cane
 *
 */
public interface Host {

    Persistence persistence(Configuration configuration) throws IOException;

    Audio audio(ResourceLoader resources, String path);

    /**
     * Show text and image. Since text and image determine the layout, they must be set simultanously.
     * 
     * @param image
     * @param text
     */
    void show(AnnotatedImage actorImage, List<String> text);

    void setFocusLevel(float focusLevel);

    /**
     * Repaint the content area to show actor image and text.
     */
    void show();

    /**
     * Shows an intertitle similar to the ones in old movies. Use it for informations that cannot be told by actors but
     * are important for the story.
     * 
     * @param text
     */
    void showInterTitle(String text);

    /**
     * Use to initiate a scene change after the current scene has been completed. This usually sort of clears the screen
     * in order to prepare for the next section.
     */
    void endScene();

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

    enum Location {
        TeaseLib,
        Host,
        User,
        Log
    }

    File getLocation(Location folder);

    void setGaze(Point2D gaze);

    void setActorProximity(HumanPose.Proximity proximity);
}
