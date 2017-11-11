package teaselib.core.media;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import teaselib.core.Audio;
import teaselib.core.Audio.Mode;
import teaselib.core.ResourceLoader;
import teaselib.core.TeaseLib;
import teaselib.core.util.ExceptionUtil;

public class RenderBackgroundSound implements MediaRenderer.Threaded {
    private static final Logger logger = LoggerFactory.getLogger(RenderBackgroundSound.class);

    private final String soundFile;
    private final Audio audio;

    private final TeaseLib teaseLib;

    private Object audioHandle = null;
    private boolean completedAll = false;

    public RenderBackgroundSound(ResourceLoader resources, String soundFile, TeaseLib teaseLib) {
        this.soundFile = soundFile;
        this.teaseLib = teaseLib;
        // TODO Make synchronous since background sound are just used because SexScript normal sounds cannot be stopped
        this.audio = teaseLib.host.audio(resources, soundFile, Mode.Background);

        try {
            audio.load();
        } catch (IOException e) {
            try {
                MediaRendererThread.handleIOException(teaseLib.config, e);
            } catch (IOException e1) {
                throw ExceptionUtil.asRuntimeException(e1);
            }
        }
    }

    @Override
    public void render() {
        teaseLib.transcript.info("Background sound = " + soundFile);
        logger.info(this.getClass().getSimpleName() + ": " + soundFile);
        completedAll = false;
        try {
            audio.play();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public String toString() {
        return soundFile;
    }

    @Override
    public void completeStart() {
        // Ignore
    }

    @Override
    public void completeMandatory() {
        // Ignore
    }

    @Override
    public void completeAll() {
        // Ignore
    }

    @Override
    public boolean hasCompletedStart() {
        return completedAll || audioHandle != null;
    }

    @Override
    public boolean hasCompletedMandatory() {
        return completedAll || audioHandle != null;
    }

    @Override
    public boolean hasCompletedAll() {
        // TODO Wait for end of background sound, flag when sound has completed
        return completedAll;
    }

    @Override
    public void interrupt() {
        audio.stop();
        completedAll = true;
    }

    @Override
    public void join() {
        // Ignore
    }
}
