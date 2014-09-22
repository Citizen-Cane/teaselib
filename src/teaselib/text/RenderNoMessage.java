package teaselib.text;

import teaselib.TeaseLib;
import teaselib.userinterface.MediaRenderer;

public class RenderNoMessage implements MediaRenderer {

	private RenderNoMessage()
	{
	}

	public static final RenderNoMessage instance = new RenderNoMessage();
	
	@Override
	public void render(TeaseLib teaseLib) {
		TeaseLib.log(getClass().getSimpleName());
		teaseLib.host.show(null);
	}
}
