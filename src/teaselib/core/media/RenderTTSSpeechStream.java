package teaselib.core.media;

import java.io.IOException;
import java.util.function.Supplier;

import teaselib.Actor;
import teaselib.Mood;
import teaselib.core.TeaseLib;
import teaselib.core.texttospeech.TextToSpeechPlayer;
import teaselib.host.Host.Audio;
import teaselib.host.Host.Audio.Control;

public class RenderTTSSpeechStream extends RenderSpeech {

    protected final Actor actor;
    private final String prompt;
    private final String mood;

    private final Audio audio;

    public RenderTTSSpeechStream(
            TextToSpeechPlayer ttsPlayer,
            Actor actor, String prompt, String mood,
            TeaseLib teaseLib, Supplier<Float> balance) throws IOException {
        super(teaseLib, balance);
        this.actor = actor;
        this.prompt = prompt;
        this.mood = mood;
        this.audio = teaseLib.audioSystem.getSound(ttsPlayer.stream(actor, prompt, mood));
        try {
            audio.load();
        } catch (IOException e) {
            handleIOException(e);
        }

    }

    @Override
    protected void renderSpeech() throws InterruptedException {
        try {
            audio.set(Control.Volume, DEFAULT_VOLUME);
            audio.set(Control.Balance, balance.get());
            audio.play();
        } catch (InterruptedException e) {
            audio.stop();
            throw e;
        } finally {
            audio.close();
        }
    }

    @Override
    public String toString() {
        return mood.equalsIgnoreCase(Mood.Neutral)
                ? ""
                : mood + ": " + "\"" + (prompt.length() > 20
                        ? prompt.substring(0, 20) + "..."
                        : prompt) + "\"";
    }

}
