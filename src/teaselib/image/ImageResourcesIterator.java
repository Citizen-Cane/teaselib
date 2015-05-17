package teaselib.image;

import java.awt.Image;
import java.io.IOException;
import java.util.List;

import teaselib.TeaseLib;

/**
 * Handles image iteration over a set of resources
 * 
 * @author someone
 */
public class ImageResourcesIterator implements ImageIterator {

	private final List<String> images;

	public ImageResourcesIterator(String path) {
		images = TeaseLib.resources().resources(path + ".+\\.jpg");
		if (images.size() == 0) {
			TeaseLib.log(getClass().getSimpleName() + ": Path '" + path
					+ "' doesn't contain any images");
		}
	}

	@Override
	public Image next() throws IOException {
		final Image image;
		if (images.size() > 0) {
			String path = images.get(TeaseLib.host().getRandom(0,
					images.size() - 1));
			image = TeaseLib.resources().image(path);
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
