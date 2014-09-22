package teaselib.image;

import teaselib.TeaseLib;
import teaselib.userinterface.MediaRenderer;

public class RenderNoImage implements MediaRenderer {

	private RenderNoImage()
	{
	}

	public static final RenderNoImage instance = new RenderNoImage();
	
	@Override
	public void render(TeaseLib teaseLib) {
		TeaseLib.log(this.getClass().getSimpleName());
		teaseLib.host.setImage(null);
	}
}
