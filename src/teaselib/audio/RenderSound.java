package teaselib.audio;

import teaselib.ScriptInterruptedException;
import teaselib.TeaseLib;
import teaselib.userinterface.MediaRendererThread;

public class RenderSound extends MediaRendererThread {

    public static final String SOUNDS = "sounds/";

    private final String soundFile;

    public RenderSound(String soundFile) {
        this.soundFile = soundFile;
    }

    @Override
    public void render() throws InterruptedException {
        try {
            String path = SOUNDS + soundFile;
            TeaseLib.log(this.getClass().getSimpleName() + ": " + path);
            startCompleted();
            teaseLib.host.playSound(path, teaseLib.resources.getResource(path));
        } catch (ScriptInterruptedException e) {
            // Expected
        } catch (Throwable e) {
            TeaseLib.log(this, e);
        } finally {
            mandatoryCompleted();
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
