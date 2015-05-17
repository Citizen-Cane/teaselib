package teaselib.util;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import teaselib.TeaseLib;
import teaselib.userinterface.MediaRenderer;

/**
 * Image is a default command. As such, every action has one, but since it
 * doesn't have a state, so we can use the same instance for all actions.
 * 
 * @author someone
 * 
 */
public class RenderDesktopItem implements MediaRenderer {

	private final String path;

	public RenderDesktopItem(String path) {
		this.path = path;
	}

	@Override
	public void render(TeaseLib teaseLib) {
		// TODO Perform in Separate task to avoid delay
		try {
			URL url = teaseLib.resources.path(path);
			try {
				URI uri = url.toURI();
				String absolutePath = new File(uri).getPath();
				Desktop.getDesktop().open(new File(absolutePath));
			} catch (URISyntaxException e) {
				TeaseLib.log(this, e);
			}
		} catch (IOException e) {
			TeaseLib.log(this, e);
		}
	}

	@Override
	public String toString() {
		return path;
	}
}
