package teaselib.core.media;

import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import teaselib.core.ScriptInterruptedException;
import teaselib.core.TeaseLib;

public class RenderDelay extends MediaRendererThread {
    private static final Logger logger = LoggerFactory.getLogger(RenderDelay.class);

    public final double seconds;
    private final boolean logToTransscript;

    public RenderDelay(double seconds, TeaseLib teaseLib) {
        this(seconds, true, teaseLib);
    }

    public RenderDelay(double seconds, boolean logToTransscript, TeaseLib teaseLib) {
        super(teaseLib);
        this.seconds = seconds;
        this.logToTransscript = logToTransscript;
    }

    @Override
    public void renderMedia() throws InterruptedException {
        startCompleted();
        try {
            if (seconds > 0) {
                if (logToTransscript) {
                    teaseLib.transcript.info("Delay=" + seconds + "s");
                }

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
