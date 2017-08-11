package teaselib.core.media;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import teaselib.core.ResourceLoader;
import teaselib.core.TeaseLib;
import teaselib.core.util.ExceptionUtil;

public class RenderBackgroundSound implements MediaRenderer.Threaded {
    private static final Logger logger = LoggerFactory.getLogger(RenderBackgroundSound.class);

    private final ResourceLoader resources;
    private final String soundFile;
    private final TeaseLib teaseLib;

    private Object audioHandle = null;
    private boolean completedAll = false;

    public RenderBackgroundSound(ResourceLoader resources, String soundFile, TeaseLib teaseLLib) {
        this.resources = resources;
        this.soundFile = soundFile;
        this.teaseLib = teaseLLib;
    }

    @Override
    public void render() throws IOException {
        teaseLib.transcript.info("Background sound = " + soundFile);
        logger.info(this.getClass().getSimpleName() + ": " + soundFile);
        try {
            completedAll = false;
            audioHandle = teaseLib.host.playBackgroundSound(resources, soundFile);
        } catch (IOException e) {
            completedAll = true;
            Exception cause = ExceptionUtil.reduce(e);
            logger.error(cause.getMessage(), cause);
        }
    }

    @Override
    public String toString() {
        return soundFile;
    }

    @Override
    public void completeStart() {
    }

    @Override
    public void completeMandatory() {
    }

    @Override
    public void completeAll() {
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
        return completedAll;
    }

    @Override
    public void interrupt() {
        if (teaseLib != null && audioHandle != null) {
            teaseLib.host.stopSound(audioHandle);
            completedAll = true;
        }
    }

    @Override
    public void join() {
    }

}
