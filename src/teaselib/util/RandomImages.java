package teaselib.util;

import java.util.List;
import java.util.NoSuchElementException;

import teaselib.ActorImages;
import teaselib.Resources;
import teaselib.core.AbstractImages;
import teaselib.util.math.Random;

/**
 * Handles image iteration over a set of resources
 * 
 * @author Citizen-Cane
 */
public class RandomImages extends AbstractImages implements ActorImages {

    private final Random random = new Random();
    private String current;

    /**
     * Builds a list of resource paths to all jpg images that match the given path.
     * 
     * @param resources
     *            The resource loader to be used to enumerate the images.
     * @param path
     *            A path to the images, without extension. todo Images have to be named path1,path2,path3.. .jpg.
     * 
     *            todo "path.jpg" won't be listed.
     */
    public RandomImages(Resources resources) {
        super(resources);
    }

    @Override
    public String next(String... hints) {
        if (!hasNext()) {
            throw new NoSuchElementException();
        } else {
            return randomImage(resources.elements);
        }
    }

    protected String randomImage(List<String> items) {
        current = random.item(current, items);
        return current;
    }

    @Override
    public void advance(Next pictures, String... hints) {
        // Ignore
    }

}
