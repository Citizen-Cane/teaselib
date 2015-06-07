package teaselib.audio;

import teaselib.TeaseLib;
import teaselib.userinterface.MediaRenderer;

public class RenderBackgroundSound implements AutoCloseable, MediaRenderer {

    private final String soundFile;
    private TeaseLib teaseLib;
    private Object handle = null;

    public RenderBackgroundSound(String soundFile) {
        this.soundFile = soundFile;
    }

    @Override
    public void render(TeaseLib teaseLib) {
        try {
            String path = soundFile;
            TeaseLib.log(this.getClass().getSimpleName() + ": " + path);
            // TODO Use the handle to allow stopping the sound
            // Implement when needed
            handle = teaseLib.host
                    .playBackgroundSound(teaseLib.resources, path);
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
