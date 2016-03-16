package teaselib.core;

import java.io.IOException;

import teaselib.Config;
import teaselib.TeaseLib;

public class RenderBackgroundSound implements MediaRenderer.Threaded {

    private final ResourceLoader resources;
    private final String soundFile;
    private final TeaseLib teaseLib;

    private Object audioHandle = null;
    private boolean completedAll = false;

    public RenderBackgroundSound(ResourceLoader resources, String soundFile,
            TeaseLib teaseLLib) {
        this.resources = resources;
        this.soundFile = soundFile;
        this.teaseLib = teaseLLib;
    }

    @Override
    public void render() throws IOException {
        teaseLib.transcript.info("Background sound = " + soundFile);
        teaseLib.log.info(this.getClass().getSimpleName() + ": " + soundFile);
        try {
            completedAll = false;
            audioHandle = teaseLib.host.playBackgroundSound(resources,
                    soundFile);
        } catch (IOException e) {
            completedAll = true;
            if (!teaseLib.getBoolean(Config.Namespace,
                    Config.Debug.IgnoreMissingResources)) {
                throw e;
            }
        }
    }

    public void stop() {
        if (teaseLib != null && audioHandle != null) {
            teaseLib.host.stopSound(audioHandle);
            completedAll = true;
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
        stop();
    }

    @Override
    public void join() {
    }

}
