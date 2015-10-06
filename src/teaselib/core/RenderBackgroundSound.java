package teaselib.core;

import java.io.IOException;

import teaselib.Config;
import teaselib.TeaseLib;

public class RenderBackgroundSound implements AutoCloseable, MediaRenderer {

    private final ResourceLoader resources;
    private final String soundFile;
    private TeaseLib teaseLib;

    private Object handle = null;

    public RenderBackgroundSound(ResourceLoader resources, String soundFile) {
        this.resources = resources;
        this.soundFile = soundFile;
    }

    @Override
    public void render(TeaseLib teaseLib) {
        try {
            TeaseLib.log(this.getClass().getSimpleName() + ": " + soundFile);
            // TODO Use the handle to allow stopping the sound
            // Implement when needed
            try {
                handle = teaseLib.host
                        .playBackgroundSound(resources, soundFile);
            } catch (IOException e) {
                if (!teaseLib.getBoolean(Config.Namespace,
                        Config.Debug.IgnoreMissingResources)) {
                    throw e;
                }
            }
        } catch (Throwable e) {
            TeaseLib.log(this, e);
        }
    }

    public void stop() {
        if (teaseLib != null && handle != null) {
            teaseLib.host.stopSound(handle);
        }
    }

    @Override
    public void close() throws Exception {
        stop();
    }

    @Override
    public String toString() {
        return soundFile;
    }
}
