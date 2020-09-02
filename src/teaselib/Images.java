package teaselib;

import java.util.Iterator;
import java.util.NoSuchElementException;

import teaselib.util.AnnotatedImage;

/**
 * Interface for iterating over a set of images. Frees script writers from having to think what image to use next.
 * Primary use is for displaying images of the dominant, where the exact image doesn't matter. However there may maybe
 * some sort of order on the images, such as undoing clothing
 * 
 * Image paths must be absolute paths.
 * 
 * @author someone
 *
 */
public interface Images extends Iterator<String> {

    public static final Images None = new Images() {
        @Override
        public boolean contains(String resource) {
            return false;
        }

        @Override
        public void hint(String... hint) { // None
        }

        @Override
        public boolean hasNext() {
            return false;
        }

        @Override
        public String next() {
            throw new NoSuchElementException();
        }

        @Override
        public AnnotatedImage annotated(String resource, byte[] image) {
            return AnnotatedImage.NoImage;
        }

        @Override
        public String toString() {
            return "[]";
        }
    };
    public static final String SameCameraPosition = "<image SameCameraPosition/>";
    public static final String SameResolution = "<image SameResolution/>";

    public boolean contains(String resource);

    /**
     * Hint the image iterator to choose an appropriate image
     * 
     * @param hint
     */
    void hint(String... hint);

    AnnotatedImage annotated(String resource, byte[] image);

}
