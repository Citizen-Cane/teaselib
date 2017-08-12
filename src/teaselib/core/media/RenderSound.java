package teaselib.core.media;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import teaselib.core.ResourceLoader;
import teaselib.core.TeaseLib;
import teaselib.core.util.ExceptionUtil;

public class RenderSound extends MediaRendererThread {
    private static final Logger logger = LoggerFactory.getLogger(RenderSound.class);

    private final ResourceLoader resources;
    private final String soundFile;

    private Object audioHandle = null;

    public RenderSound(ResourceLoader resources, String soundFile, TeaseLib teaseLib) {
        super(teaseLib);
        this.resources = resources;
        this.soundFile = soundFile;
    }

    @Override
    public void renderMedia() throws IOException, InterruptedException {
        try {
            teaseLib.transcript.info("Message sound = " + soundFile);
            logger.info(this.getClass().getSimpleName() + ": " + soundFile);
            startCompleted();
            try {
                audioHandle = soundFile;
                teaseLib.host.playSound(resources, soundFile);
                logger.info(this.getClass().getSimpleName() + ": " + soundFile + " completed");
            } catch (IOException e) {
                handleIOException(ExceptionUtil.reduce(e));
            }
        } finally {
            mandatoryCompleted();
        }
    }

    @Override
    public String toString() {
        return soundFile;
    }

    @Override
    public void interrupt() {
        if (audioHandle != null) {
            teaseLib.host.stopSound(audioHandle);
        }
        super.interrupt();
    }
}
