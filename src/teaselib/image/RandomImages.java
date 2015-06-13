package teaselib.image;

import java.util.List;

import teaselib.ResourceLoader;
import teaselib.TeaseLib;

/**
 * Handles image iteration over a set of resources
 * 
 * @author someone
 */
public class RandomImages implements Images {

    protected final List<String> images;

    /**
     * Builds a list of resource paths to all jpg images that match the given
     * path.
     * 
     * @param resources
     *            The resource loader to be used to enumerate the images.
     * @param path
     *            A path to the images, without extension. todo Images have to
     *            be named path1,path2,path3.. .jpg.
     * 
     *            todo "path.jpg" won't be listed.
     */
    public RandomImages(ResourceLoader resources, String path) {
        // todo document regex patterns, they're just to hard to remember
        String filesWithImageExtension = path + ".+\\.jpg";
        String pathsThatEndWithAnImageExtension = "(.*)("
                + filesWithImageExtension + ")$";
        images = resources.resources(pathsThatEndWithAnImageExtension);
        if (images.size() == 0) {
            TeaseLib.log(getClass().getSimpleName() + ": Path '" + path
                    + "' doesn't contain any images");
        }
    }

    @Override
    public String next() {
        return getRandomResource(images);
    }

    protected String getRandomResource(List<String> selection) {
        final String path;
        if (selection.size() > 0) {
            int i = (int) (Math.random() * selection.size());
            path = selection.get(i);
        } else {
            // Empty image set
            path = null;
        }
        return path;
    }

    @Override
    public void hint(String... hint) {
    }

}
