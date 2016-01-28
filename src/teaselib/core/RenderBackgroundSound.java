package teaselib.core;

import java.io.IOException;

import teaselib.Config;
import teaselib.TeaseLib;

public class RenderBackgroundSound implements AutoCloseable, MediaRenderer {

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
        // TODO Use the handle to allow stopping the sound
        try {
            teaseLib.host.playBackgroundSound(resources, soundFile);
            audioHandle = soundFile;
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
    public void close() {
        stop();
    }

    @Override
    public String toString() {
        return soundFile;
    }
}
