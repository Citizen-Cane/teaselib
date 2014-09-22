package teaselib.image;

import java.io.IOException;

import teaselib.TeaseLib;
import teaselib.userinterface.MediaRenderer;

/**
 * Image is a default command. As such, every action has one, but since it
 * doesn't have a state, so we can use the same instance for all actions.
 * 
 * @author someone
 * 
 */
public class RenderImage implements MediaRenderer {

	private static final String IMAGES = "images/";

	private final String path;

	public RenderImage(String path) {
		this.path = path;
	}

	@Override
	public void render(TeaseLib teaseLib) {
		java.awt.Image image = null;
		try {
			image = teaseLib.resources.image(IMAGES + path);
			TeaseLib.log(this.getClass().getSimpleName() + ": "+ path);
		} catch (IOException e) {
			// throw new ScriptError("Unable to display image", e);
			TeaseLib.log(this, e);
		}
		teaseLib.host.setImage(image);
	}

	@Override
	public String toString() {
		return path;
	}
}
