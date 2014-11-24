package teaselib.audio;

import teaselib.TeaseLib;
import teaselib.userinterface.MediaRenderer;

public class RenderSound implements MediaRenderer {

	private static final String SOUNDS = "sounds/";

	private final String soundFile;

	public RenderSound(String soundFile) {
		this.soundFile = soundFile;
	}

	@Override
	public void render(TeaseLib teaseLib) {
		try {
			String path = SOUNDS + soundFile;
			TeaseLib.log(this.getClass().getSimpleName() + ": " + path);
			teaseLib.host.playBackgroundSound(path,
					teaseLib.resources.getResource(path));
		} catch (Throwable e) {
			TeaseLib.log(this, e);
		}
	}

	@Override
	public String toString() {
		return soundFile;
	}
}
