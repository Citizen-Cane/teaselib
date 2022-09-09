package teaselib.core.media;

import java.io.IOException;

import teaselib.core.ResourceLoader;
import teaselib.core.TeaseLib;
import teaselib.host.Host.Audio;

public class RenderPrerecordedSpeech extends RenderSpeech {

    private final String speechSoundFile;
    private final Audio audio;

    public RenderPrerecordedSpeech(String speechSoundFile, ResourceLoader resources, TeaseLib teaseLib)
            throws IOException {
        super(teaseLib);
        this.speechSoundFile = speechSoundFile;
        this.audio = teaseLib.audioSystem.getSound(resources, speechSoundFile);
        try {
            audio.load();
        } catch (IOException e) {
            handleIOException(e);
        }
    }

    @Override
    protected void renderSpeech() throws IOException, InterruptedException {
        try {
            audio.play();
        } catch (InterruptedException e) {
            audio.stop();
            throw e;
        }
    }

    @Override
    public String toString() {
        return speechSoundFile;
    }

}
