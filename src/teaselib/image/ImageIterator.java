package teaselib.image;

import java.awt.Image;
import java.io.IOException;

/**
 * Interface for iterating over a set of images. Frees script writers from
 * having to think what image to use next. Primary use is for displaying
 * images of the dominant, where the exact image doesn't matter. However there may
 * maybe some sort of order on the images, such as undoing clothing
 * 
 * @author someone
 *
 */
public interface ImageIterator {

	public static final String SameCameraPosition = "<image SameCameraPosition/>";
	public static final String SameResolution = "<image SameResolution/>";

	/**
	 * Returns the next image in the sequence
	 * 
	 * @return Image object with the next image
	 */
	Image next() throws IOException;

	/**
	 * Hint the image iterator to choose an appropriate image
	 * 
	 * @param hint
	 */
	void hint(String... hint);
}
