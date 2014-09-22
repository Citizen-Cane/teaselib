package teaselib.image;

import java.awt.Image;
import java.io.IOException;

/**
 * Interface for iterating over a set of images.
 * Frees script writers from having to think what image to use next.
 * Primary use is for displaying "Domme" or "Dom" images, where the exact image doesn't matter,
 * However there may maybe some sort of order on the images, like undoing clothing
 * 
 * @author someone
 *
 */
public interface ImageIterator {
	/**
	 * Returns the next image in the sequence
	 * @return Image object with the next image
	 */
	Image next() throws IOException;
}
