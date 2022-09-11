package teaselib.host.sexscripts;

import java.io.IOException;

import ss.IScript;
import teaselib.core.ResourceLoader;
import teaselib.host.Host.Audio;

final class SexScriptsAudio implements Audio {

    private final IScript ss;
    private final ResourceLoader resources;
    private final String path;

    String audioHandle = null;

    SexScriptsAudio(IScript ss, ResourceLoader resources, String path) {
        this.resources = resources;
        this.path = path;
        this.ss = ss;
    }

    @Override
    public void load() throws IOException {
        this.audioHandle = resources.unpackToFile(path).getAbsolutePath();
    }

    @Override
    public void set(Control control, float value) { /* Ignore */ }

    @Override
    public void play() throws InterruptedException {
        // TODO interrupting the thread has no effect
        // TODO Doesn't react to stopSoundThreads() either
        // TODO sometimes displays dialog telling "Sleep Interrupted" (when playing mp3 sound)
        ss.playSound(audioHandle);
    }

    @Override
    public void stop() {
        try {
            ss.stopSoundThreads();
        } catch (Exception e) {
            SexScriptsHost.logger.error(e.getMessage(), e);
        }
    }

    @Override
    public void close() {
        // Ignore
    }

}
