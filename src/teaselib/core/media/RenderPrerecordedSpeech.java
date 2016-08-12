package teaselib.core.media;

import java.io.IOException;

import teaselib.TeaseLib;
import teaselib.core.ResourceLoader;

public class RenderPrerecordedSpeech extends RenderSpeech {
    private final ResourceLoader resources;
    private final String speechSoundFile;

    private Object audioHandle = null;

    public RenderPrerecordedSpeech(String speechSoundFile, long pauseMillis,
            ResourceLoader resources, TeaseLib teaseLib) {
        super(pauseMillis, teaseLib);
        this.resources = resources;
        this.speechSoundFile = speechSoundFile;
    }

    @Override
    protected void renderSpeech() throws IOException, InterruptedException {
        audioHandle = speechSoundFile;
        teaseLib.host.playSound(resources, speechSoundFile);
    }

    @Override
    public String toString() {
        return speechSoundFile;
    }

    @Override
    public void interrupt() {
        if (audioHandle != null) {
            teaseLib.host.stopSound(audioHandle);
        }
        super.interrupt();
    }
}
