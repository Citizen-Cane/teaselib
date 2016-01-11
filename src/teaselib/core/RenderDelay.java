package teaselib.core;

import java.util.concurrent.TimeUnit;

public class RenderDelay extends MediaRendererThread {
    public final int seconds;

    public RenderDelay(int seconds) {
        this.seconds = seconds;
    }

    @Override
    public void render() throws InterruptedException {
        teaseLib.transcript.info("Message delay = " + seconds + " seconds");
        startCompleted();
        try {
            if (seconds > 0) {
                teaseLib.log.info(getClass().getSimpleName() + " " + toString()
                        + ": " + seconds + " seconds");
                teaseLib.sleep(seconds, TimeUnit.SECONDS);
            } else {
                teaseLib.log.info(getClass().getSimpleName() + " " + toString()
                        + ": skipped sleeping " + seconds + " seconds");
            }
        } catch (ScriptInterruptedException e) {
            // Expected
        } finally {
            mandatoryCompleted();
        }
    }

    @Override
    public String toString() {
        return Integer.toString(seconds);
    }
}
