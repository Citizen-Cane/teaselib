package teaselib;

import teaselib.userinterface.MediaRendererQueue;


public abstract class TeaseScriptBase {

	public final TeaseLib teaseLib;

	public final MediaRendererQueue renderQueue = new MediaRendererQueue();
	
	public TeaseScriptBase(TeaseLib teaseLib) {
		this.teaseLib = teaseLib;
	}

	/**
	 * Just wait for everything to be rendered (messages displayed, sounds
	 * played, delay expired), and continue execution of the script. This won't
	 * display a button, it just waits.
	 */
	public void completeAll() {
		renderQueue.completeAll();
	}

	/**
	 * Workaround as of now because PCMPlayer must display the stop button immediately
	 */
	public void completeMandatory() {
		renderQueue.completeAllMandatories();
	}

}
