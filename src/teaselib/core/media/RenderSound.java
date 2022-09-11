package teaselib.core.media;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import teaselib.core.ResourceLoader;
import teaselib.core.TeaseLib;
import teaselib.host.Host.Audio;
import teaselib.host.Host.Audio.Control;

public class RenderSound extends MediaRendererThread {

    private static final Logger logger = LoggerFactory.getLogger(RenderSound.class);

    public static class Background extends RenderSound {
        private static final float DEFAULT_VOLUME = 0.5f;

        public Background(ResourceLoader resources, String soundFile, TeaseLib teaseLib) throws IOException {
            super(resources, soundFile, teaseLib);
            audio.set(Control.Volume, DEFAULT_VOLUME);
            audio.set(Control.Balance, 0.0f);
        }
    }

    public static class Foreground extends RenderSound {
        private static final float DEFAULT_VOLUME = 1.0f;

        public Foreground(ResourceLoader resources, String soundFile, TeaseLib teaseLib) throws IOException {
            super(resources, soundFile, teaseLib);
            audio.set(Control.Volume, DEFAULT_VOLUME);
            audio.set(Control.Balance, 0.0f);
        }
    }

    final String soundFile;
    final Audio audio;

    private RenderSound(ResourceLoader resources, String soundFile, TeaseLib teaseLib) throws IOException {
        super(teaseLib);
        this.soundFile = soundFile;
        this.audio = teaseLib.audioSystem.getSound(resources, soundFile);
        try {
            audio.load();
        } catch (IOException e) {
            handleIOException(e);
        }
    }

    @Override
    public void renderMedia() throws IOException, InterruptedException {
        try {
            teaseLib.transcript.info("Sound = " + soundFile);
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
