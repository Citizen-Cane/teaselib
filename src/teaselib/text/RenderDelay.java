package teaselib.text;

import teaselib.ScriptInterruptedException;
import teaselib.TeaseLib;
import teaselib.userinterface.MediaRendererThread;

public class RenderDelay extends MediaRendererThread {
	public final int from;
	public final int to;

	public RenderDelay(int delay) {
		from = to = delay;
	}

	public RenderDelay(int from, int to) {
		this.from = from;
		this.to = to;
	}

	@Override
	public void render() throws InterruptedException {
		int actual = teaseLib.host.getRandom(from, to);
		TeaseLib.log(getClass().getSimpleName() + " " + toString() + ": " + actual + " seconds");
		synchronized(completedMandatoryParts)
		{
			try {
				teaseLib.host.sleep(actual * 1000);
			} catch (ScriptInterruptedException e) {
				// Expected
			}
		}
	}

	@Override
	public String toString() {
		return from + "-"+ to;
	}
}
