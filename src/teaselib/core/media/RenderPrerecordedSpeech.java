package teaselib.core.media;

import java.io.IOException;
import java.util.function.Supplier;

import teaselib.core.ResourceLoader;
import teaselib.core.TeaseLib;
import teaselib.host.Host.Audio;
import teaselib.host.Host.Audio.Control;

public class RenderPrerecordedSpeech extends RenderSpeech {

    private final String speechSoundFile;
    private final Audio audio;

    public RenderPrerecordedSpeech(String speechSoundFile, ResourceLoader resources, TeaseLib teaseLib, Supplier<Float> balance)
            throws IOException {
        super(teaseLib, balance);
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
            audio.set(Control.Volume, DEFAULT_VOLUME);
            audio.set(Control.Balance, balance.get());
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
