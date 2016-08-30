package teaselib.core.media;

import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import teaselib.core.ScriptInterruptedException;
import teaselib.core.TeaseLib;

public class RenderDelay extends MediaRendererThread {
    private static final Logger logger = LoggerFactory
            .getLogger(RenderDelay.class);
    public final int seconds;

    public RenderDelay(int seconds, TeaseLib teaseLib) {
        super(teaseLib);
        this.seconds = seconds;
    }

    @Override
    public void renderMedia() throws InterruptedException {
        teaseLib.transcript.info("Message delay = " + seconds + " seconds");
        startCompleted();
        try {
            if (seconds > 0) {
                logger.info(getClass().getSimpleName() + " " + toString()
                        + ": " + seconds + " seconds");
                teaseLib.sleep(seconds, TimeUnit.SECONDS);
            } else {
                logger.info(getClass().getSimpleName() + " " + toString()
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
