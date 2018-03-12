package teaselib.core.media;

import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import teaselib.core.ScriptInterruptedException;
import teaselib.core.TeaseLib;

public class RenderDelay extends MediaRendererThread {
    private static final Logger logger = LoggerFactory.getLogger(RenderDelay.class);
    public final double seconds;

    public RenderDelay(double seconds, TeaseLib teaseLib) {
        super(teaseLib);
        this.seconds = seconds;
    }

    @Override
    public void renderMedia() throws InterruptedException {
        teaseLib.transcript.info("Message delay = " + seconds + " seconds");
        startCompleted();
        try {
            if (seconds > 0) {
                logger.info("Sleeping {} seconds", seconds);
                teaseLib.sleep((long) (seconds * 1000), TimeUnit.MILLISECONDS);
            } else {
                logger.info("skipped sleeping {} seconds", +seconds);
            }
        } catch (ScriptInterruptedException e) {
            // Expected
        } finally {
            mandatoryCompleted();
        }
    }

    @Override
    public String toString() {
        return Double.toString(seconds);
    }
}
