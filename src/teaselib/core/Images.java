package teaselib.core;

import java.util.Iterator;

/**
 * Interface for iterating over a set of images. Frees script writers from
 * having to think what image to use next. Primary use is for displaying images
 * of the dominant, where the exact image doesn't matter. However there may
 * maybe some sort of order on the images, such as undoing clothing
 * 
 * Image paths must be absolute paths.
 * 
 * @author someone
 *
 */
public interface Images extends Iterator<String> {

    public static final String SameCameraPosition = "<image SameCameraPosition/>";
    public static final String SameResolution = "<image SameResolution/>";

    public boolean contains(String resource);

    /**
     * Hint the image iterator to choose an appropriate image
     * 
     * @param hint
     */
    void hint(String... hint);
}
