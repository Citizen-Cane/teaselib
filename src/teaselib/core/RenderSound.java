package teaselib.core;

import java.io.IOException;

import teaselib.Config;
import teaselib.TeaseLib;

public class RenderSound extends MediaRendererThread {
    private final ResourceLoader resources;
    private final String soundFile;

    private Object audioHandle = null;

    public RenderSound(ResourceLoader resources, String soundFile,
            TeaseLib teaseLib) {
        super(teaseLib);
        this.resources = resources;
        this.soundFile = soundFile;
    }

    @Override
    public void renderMedia() throws IOException {
        try {
            teaseLib.transcript.info("Message sound = " + soundFile);
            teaseLib.log
                    .info(this.getClass().getSimpleName() + ": " + soundFile);
            startCompleted();
            try {
                audioHandle = soundFile;
                teaseLib.host.playSound(resources, soundFile);
                teaseLib.log.info(this.getClass().getSimpleName() + ": "
                        + soundFile + " completed");
            } catch (ScriptInterruptedException e) {
                // Expected
            } catch (IOException e) {
                if (!teaseLib.getBoolean(Config.Namespace,
                        Config.Debug.IgnoreMissingResources)) {
                    throw e;
                }
            }
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
        if (audioHandle != null) {
            teaseLib.host.stopSound(audioHandle);
        }
        super.interrupt();
    }
}
