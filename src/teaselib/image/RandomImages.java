package teaselib.image;

import java.awt.Image;
import java.io.IOException;
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
    private final ResourceLoader resources;

    public RandomImages(ResourceLoader resources, String path) {
        images = resources.resources(path + ".+\\.jpg");
        this.resources = resources;
        if (images.size() == 0) {
            TeaseLib.log(getClass().getSimpleName() + ": Path '" + path
                    + "' doesn't contain any images");
        }
    }

    @Override
    public Image next() throws IOException {
        final Image image;
        image = getRandomImage(images);
        return image;
    }

    protected Image getRandomImage(List<String> selection) throws IOException {
        final Image image;
        if (selection.size() > 0) {
            int i = (int) (Math.random() * selection.size());
            String path = selection.get(i);
            image = resources.image(path);
        } else {
            // Empty image set
            image = null;
        }
        return image;
    }

    @Override
    public void hint(String... hint) {
    }

}
