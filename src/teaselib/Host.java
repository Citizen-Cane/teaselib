package teaselib;

import java.awt.Image;
import java.io.InputStream;
import java.util.List;

import teaselib.util.Delegate;

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
	 * Get a random number between min and max (included)
	 * 
	 * @param min
	 *            Minimum desired value
	 * @param max
	 *            Maximum desired value
	 * @return Random value from [min, max]
	 */
	int getRandom(int min, int max);

	/**
	 * @return Time in seconds starting from 1.1.1970
	 */
	long getTime();

	void log(String line);

	void playSound(String file);

	void playBackgroundSound(String path, InputStream inputStream);

	void setImage(Image image);

	void show(String message);

	/**
	 * Show formatted text that is not spoken, often used as a hint to the user
	 * 
	 * @param text
	 */
	void showText(String text);

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
			List<Boolean> values);

	/**
	 * Stop any playing background sounds
	 */
	void stopSounds();

	/**
	 * Preferred method to wait, since it allows us to write a debug host with
	 * automated input. If interrupted, must throw a ScriptInterruptedException.
	 * It's a runtime exception so it doesn't have to be declared. This way
	 * simple scripts are safe, but script closures can be cancelled.
	 * 
	 * May also be called with Infinitely in order to stop execution.
	 * @param milliseconds The time to sleep.
	 */
	void sleep(long milliseconds);

	/**
	 * Get a list of delegates, one for each user interaction. The index is the
	 * same as for showInteraction.
	 * 
	 * @return List of delegates, run the appropriate delegate to perform the
	 *         user interaction of the corresponding user interface element.
	 */
	List<Delegate> getClickableChoices(List<String> choices);

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
	 */
	int choose(List<String> choices);
}
