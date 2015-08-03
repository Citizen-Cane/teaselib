package teaselib.core;


/**
 * Interface for iterating over a set of images. Frees script writers from
 * having to think what image to use next. Primary use is for displaying images
 * of the dominant, where the exact image doesn't matter. However there may
 * maybe some sort of order on the images, such as undoing clothing
 * 
 * @author someone
 *
 */
public interface Images {

    public static final String SameCameraPosition = "<image SameCameraPosition/>";
    public static final String SameResolution = "<image SameResolution/>";

    /**
     * Returns the next image in the sequence
     * 
     * @return Resource path to the next image
     */
    String next();

    /**
     * Hint the image iterator to choose an appropriate image
     * 
     * @param hint
     */
    void hint(String... hint);
}