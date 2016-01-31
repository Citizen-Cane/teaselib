package teaselib.core;

import java.io.IOException;

import teaselib.Config;
import teaselib.TeaseLib;

public class RenderBackgroundSound implements MediaRenderer.Threaded {

    private final ResourceLoader resources;
    private final String soundFile;
    private TeaseLib teaseLib = null;

    private Object audioHandle = null;

    public RenderBackgroundSound(ResourceLoader resources, String soundFile) {
        this.resources = resources;
        this.soundFile = soundFile;
    }

    @Override
    public void render(TeaseLib teaseLib) throws IOException {
        this.teaseLib = teaseLib;
        teaseLib.transcript.info("Background sound = " + soundFile);
        teaseLib.log.info(this.getClass().getSimpleName() + ": " + soundFile);
        try {
            audioHandle = teaseLib.host.playBackgroundSound(resources,
                    soundFile);
        } catch (IOException e) {
            if (!teaseLib.getBoolean(Config.Namespace,
                    Config.Debug.IgnoreMissingResources)) {
                throw e;
            }
        }
    }

    public void stop() {
        if (teaseLib != null && audioHandle != null) {
            teaseLib.host.stopSound(audioHandle);
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
        // TODO Auto-generated method stub
        return true;
    }

    @Override
    public boolean hasCompletedMandatory() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean hasCompletedAll() {
        return false;
    }

    @Override
    public void interrupt() {
        stop();
    }

    @Override
    public void join() {
    }

}
