package teaselib.core.media;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import teaselib.core.Audio;
import teaselib.core.ResourceLoader;
import teaselib.core.TeaseLib;

public class RenderSound extends MediaRendererThread {
    private static final Logger logger = LoggerFactory.getLogger(RenderSound.class);

    private final String soundFile;
    private final Audio audio;

    public RenderSound(ResourceLoader resources, String soundFile, TeaseLib teaseLib) {
        super(teaseLib);
        this.soundFile = soundFile;
        this.audio = teaseLib.host.audio(resources, soundFile);

        try {
            audio.load();
        } catch (IOException e) {
            handleIOException(e);
        }
    }

    @Override
    public void renderMedia() throws IOException, InterruptedException {
        try {
            teaseLib.transcript.info("Message sound = " + soundFile);
            logger.info("{} started", soundFile);
            startCompleted();
            audio.play();
            logger.info("{} completed", soundFile);
        } catch (InterruptedException e) {
            audio.stop();
            throw e;
        }
    }

    @Override
    public String toString() {
        return soundFile;
    }

}
