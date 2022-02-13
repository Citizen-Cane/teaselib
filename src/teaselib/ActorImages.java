package teaselib;

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
public interface ActorImages extends Images {

    public static final ActorImages None = new ActorImages() {
        @Override
        public boolean contains(String resource) {
            return false;
        }

        @Override
        public boolean hasNext() {
            return false;
        }

        @Override
        public String next(String... hints) {
            throw new NoSuchElementException();
        }

        @Override
        public void fetch(String resource) {
            // Ignore
        }

        @Override
        public AnnotatedImage annotated(String resource) {
            return AnnotatedImage.NoImage;
        }

        @Override
        public String toString() {
            return "[]";
        }
    };

    public static final String SameCameraPosition = "<image SameCameraPosition/>";
    public static final String SameResolution = "<image SameResolution/>";

    public boolean hasNext();

    public String next(String... hints);

}
