package teaselib.core.media;

import java.io.IOException;

import teaselib.core.Audio;
import teaselib.core.ResourceLoader;
import teaselib.core.TeaseLib;

public class RenderPrerecordedSpeech extends RenderSpeech {
    private final String speechSoundFile;
    private final Audio audio;

    public RenderPrerecordedSpeech(String speechSoundFile, ResourceLoader resources, TeaseLib teaseLib)
            throws IOException {
        super(teaseLib);
        this.speechSoundFile = speechSoundFile;
        this.audio = teaseLib.host.audio(resources, speechSoundFile);

        audio.load();
    }

    @Override
    protected void renderSpeech() throws IOException, InterruptedException {
        audio.play();
    }

    @Override
    public String toString() {
        return speechSoundFile;
    }

    @Override
    public void interrupt() {
        audio.stop();
        super.interrupt();
    }
}
