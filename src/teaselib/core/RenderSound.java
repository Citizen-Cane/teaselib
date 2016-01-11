package teaselib.core;

import java.io.IOException;

import teaselib.Config;

public class RenderSound extends MediaRendererThread {
    private final ResourceLoader resources;
    private final String soundFile;

    public RenderSound(ResourceLoader resources, String soundFile) {
        this.resources = resources;
        this.soundFile = soundFile;
    }

    @Override
    public void render() throws IOException {
        try {
            teaseLib.transcript.info("Message sound = " + soundFile);
            teaseLib.log.info(this.getClass().getSimpleName() + ": "
                    + soundFile);
            startCompleted();
            try {
                teaseLib.host.playSound(resources, soundFile);
            } catch (IOException e) {
                if (!teaseLib.getBoolean(Config.Namespace,
                        Config.Debug.IgnoreMissingResources)) {
                    throw e;
                }
            }
        } catch (ScriptInterruptedException e) {
            // Expected
        } finally {
            mandatoryCompleted();
            teaseLib.log.info(this.getClass().getSimpleName() + ": "
                    + soundFile + " completed");
        }
    }

    @Override
    public String toString() {
        return soundFile;
    }

    @Override
    public void join() {
        teaseLib.host.stopSounds();
        // TODO Only stop speech and sounds that
        // have been stated by this sound renderer
        super.join();
    }

}
