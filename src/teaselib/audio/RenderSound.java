package teaselib.audio;

import teaselib.TeaseLib;
import teaselib.userinterface.MediaRendererThread;
import teaselib.util.ScriptInterruptedException;

public class RenderSound extends MediaRendererThread {
    private final String soundFile;

    public RenderSound(String soundFile) {
        this.soundFile = soundFile;
    }

    @Override
    public void render() throws InterruptedException {
        try {
            TeaseLib.log(this.getClass().getSimpleName() + ": " + soundFile);
            startCompleted();
            teaseLib.host.playSound(teaseLib.resources, soundFile);
        } catch (ScriptInterruptedException e) {
            // Expected
        } catch (Throwable e) {
            TeaseLib.log(this, e);
        } finally {
            mandatoryCompleted();
            TeaseLib.log(this.getClass().getSimpleName() + ": " + soundFile
                    + " completed");
        }
    }

    @Override
    public String toString() {
        return soundFile;
    }

    @Override
    public void end() {
        teaseLib.host.stopSounds();
        // TODO Only stop speech and sounds that
        // have been stated by this sound renderer
        super.end();
    }

}
