package teaselib.text;

import java.util.concurrent.TimeUnit;

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
        int actual = teaseLib.random(from, to);
        startCompleted();
        try {
            if (actual > 0) {
                TeaseLib.log(getClass().getSimpleName() + " " + toString()
                        + ": " + actual + " seconds");
                teaseLib.sleep(actual, TimeUnit.SECONDS);
            } else {
                TeaseLib.log(getClass().getSimpleName() + " " + toString()
                        + ": skipped sleeping " + actual + " seconds");
            }
        } catch (ScriptInterruptedException e) {
            // Expected
        } finally {
            mandatoryCompleted();
        }
    }

    @Override
    public String toString() {
        return from + "-" + to;
    }
}
