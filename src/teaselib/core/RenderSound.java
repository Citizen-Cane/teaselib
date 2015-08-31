package teaselib.core;

import teaselib.TeaseLib;

public class RenderSound extends MediaRendererThread {
    private final ResourceLoader resources;
    private final String soundFile;

    public RenderSound(ResourceLoader resources, String soundFile) {
        this.resources = resources;
        this.soundFile = soundFile;
    }

    @Override
    public void render() throws InterruptedException {
        try {
            TeaseLib.log(this.getClass().getSimpleName() + ": " + soundFile);
            startCompleted();
            teaseLib.host.playSound(resources, soundFile);
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
    public void join() {
        teaseLib.host.stopSounds();
        // TODO Only stop speech and sounds that
        // have been stated by this sound renderer
        super.join();
    }

}
