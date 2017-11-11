package teaselib.core.media;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import teaselib.core.Audio;
import teaselib.core.Audio.Mode;
import teaselib.core.ResourceLoader;
import teaselib.core.TeaseLib;
import teaselib.core.util.ExceptionUtil;

public class RenderSound extends MediaRendererThread {
    private static final Logger logger = LoggerFactory.getLogger(RenderSound.class);

    private final String soundFile;
    private final Audio audio;

    public RenderSound(ResourceLoader resources, String soundFile, TeaseLib teaseLib) {
        super(teaseLib);
        this.soundFile = soundFile;
        this.audio = teaseLib.host.audio(resources, soundFile, Mode.Synchronous);

        try {
            audio.load();
        } catch (IOException e) {
            try {
                handleIOException(e);
            } catch (IOException e1) {
                ExceptionUtil.asRuntimeException(e1);
            }
        }
    }

    @Override
    public void renderMedia() throws IOException, InterruptedException {
        try {
            teaseLib.transcript.info("Message sound = " + soundFile);
            logger.info(this.getClass().getSimpleName() + ": " + soundFile);
            startCompleted();
            audio.play();
            logger.info(this.getClass().getSimpleName() + ": " + soundFile + " completed");
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
        audio.stop();
        super.interrupt();
    }
}
