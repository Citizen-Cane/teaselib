package teaselib.image;

import java.awt.Image;
import java.io.IOException;

import teaselib.TeaseLib;
import teaselib.userinterface.MediaRenderer;

/**
 * MistressImage is a default command. As such, every action has one (if no other specified),
 * but since it doesn't have a state, we can use the same instance for all actions.
 * @author someone
 *
 */
public class RenderImageIterator implements MediaRenderer {

	private final ImageIterator imageIterator;
	
	public RenderImageIterator(ImageIterator imageIterator)
	{
		this.imageIterator = imageIterator;
	}

	@Override
	public void render(TeaseLib teaseLib) {
		try {
			Image image = imageIterator.next();
			if (image == null)
			{
				TeaseLib.log(imageIterator.getClass().getSimpleName() + ": image not found");
			}
			teaseLib.host.setImage(image);
		} catch (IOException e) {
			TeaseLib.log(this, e);
			teaseLib.host.setImage(null);
		}
	}
}
